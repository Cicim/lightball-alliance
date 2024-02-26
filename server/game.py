import asyncio
import random
import math
from dataclasses import dataclass
from aiohttp import web


@dataclass
class Vec3:
    x: float
    y: float
    z: float

    def set(self, x: float, y: float, z: float):
        self.x = x
        self.y = y
        self.z = z

    def copy(self, other: 'Vec3 | None' = None) -> 'Vec3':
        if other is None:
            return Vec3(self.x, self.y, self.z)

        self.x = other.x
        self.y = other.y
        self.z = other.z
        return self

    def distance(self, other: 'Vec3') -> float:
        return ((self.x - other.x)**2 + (self.y - other.y)**2 + (self.z - other.z)**2)**0.5

    def __str__(self):
        return f"({self.x:.2f}, {self.y:.2f}, {self.z:.2f})"

    def dict(self):
        return {
            'x': self.x,
            'y': self.y,
            'z': self.z
        }


@dataclass
class Enemy:
    # Enemy identifier
    id: int
    # Health of the enemy (hits to kill it)
    health: int
    # Color of the enemy
    color: int

    # Starting position in world coordinates
    source: Vec3
    # Ending position in world coordinates
    target: Vec3
    # Time at which the enemy was created
    start_time: int
    # Speed of the enemy in units per second
    speed: float

    # Current position of the enemy
    def get_position(self, time: int) -> Vec3:
        return Vec3(
            self.source.x + (self.target.x - self.source.x) *
            (time - self.start_time) * self.speed,
            self.source.y + (self.target.y - self.source.y) *
            (time - self.start_time) * self.speed,
            self.source.z + (self.target.z - self.source.z) *
            (time - self.start_time) * self.speed
        )


class Player:
    # Player identifier
    username: str
    # Player health
    health: int
    # Accumulated score
    score: int
    # If the player is ready to start
    ready: bool

    # Player rotation vector (for the observer)
    rotation: Vec3
    # Player position in world coordinates (always constant)
    position: Vec3

    def __init__(self, username: str):
        self.username = username
        self.rotation = Vec3(0, 0, 0)
        self.position = Vec3(0, 0, 0)
        self.health = 100
        self.score = 0
        self.ready = False

    def dict(self):
        return {
            'username': self.username,
            'health': self.health,
            'score': self.score,
            'position': self.position.dict(),
            'rotation': self.rotation.dict()
        }


class Logger:
    identifier: int
    time: int

    def __init__(self, identifier: int = 0):
        self.identifier = identifier
        self.time = 0

    def print_header(self):
        if self.identifier > 0:
            print(
                f"[Game {self.identifier} - {self.time/1000:>7.2f}s]", end=" ")
        else:
            print(f"[Server Message]", end=" ")

    def log(self, message: str):
        self.print_header()

        # Print just the message in blue
        print(f"\033[92m{message}\033[0m")

    def log_error(self, message: str):
        print("\033[91m", end='')
        self.print_header()

        # Print the error message in red
        print(f"ERROR: {message}\033[0m")

    def log_broadcast(self, name: str, message: str, highlight: bool = True):
        self.print_header()

        # Split the message by line
        lines = message.split('\n')
        # Trim trailing empty lines
        while len(lines) > 0 and lines[-1] == '':
            lines.pop()

        if highlight:
            # Print the broadcast message in blue
            print(f"Broadcast \033[94m{name}\033[0m:", end=' ')
        else:
            # Print the message in gray
            print(f"\033[90mBroadcast {name}\033[0m:", end=' ')

        # Print all the lines in gray
        if not highlight:
            print("\033[90m", end='')

        if len(lines) > 1:
            print()
            for line in lines:
                print(f"                       {line}")
        else:
            print(message)

        if not highlight:
            print("\033[0m", end='')


class GameBroadcasts:
    """This class is used to define the events and broadcasts that the game can send to the clients. 
    It is used to define the interface of the game, and to allow the game to send events and broadcasts
    to the clients without knowing the implementation of the clients."""

    # Game id for the logs
    logger: Logger
    # List of clients connected to the game
    clients: list[web.WebSocketResponse]

    def __init__(self, clients: list[web.WebSocketResponse] = [], logger: Logger = Logger(0)):
        self.clients = clients
        self.logger = logger

    async def _broadcast(self, msg_type: str, msg_data: dict | str | int):
        """Send a message to all the clients."""
        try:
            # Send all **in parallel** and wait for all to finish before continuing
            await asyncio.gather(*[client.send_json({'type': msg_type, 'data': msg_data}) for client in self.clients])
        except Exception as e:
            self.logger.log_error(
                f"Failed to broadcast to all clients")

    # Broadcasts
    # Broadcast to all the players that the game has started with the list of players.
    async def game_started(self, players: list[Player]):
        message = ""
        for player in players:
            message += f" - Player {player.username:<16} {player.health} HP, position {player.position}\n"

        self.logger.log_broadcast("game_started", message)

        await self._broadcast('game_started', {
            'players': [player.dict() for player in players]
        })

    # Game over broadcast with motivation
    async def game_over(self, motivation: str):
        self.logger.log_broadcast("game_over", motivation)

        for client in self.clients:
            # Game over is a special case, because if the game over is due to a player
            # disconnecting, we don't want the broadcast to fail when targeting the
            # disconnected transport. We just ignore the exception.
            try:
                await client.send_json({'type': 'game_over', 'data': motivation})
            except:
                pass

    # Event for time synchronization

    async def time_sync(self, time: int):
        self.logger.log_broadcast(
            "time_sync", f"{time / 1000.0:.3f}s", highlight=False)

        await self._broadcast('time_sync', time)

    # A new enemy has been added to the game
    async def enemy_added(self, enemy: Enemy):
        self.logger.log_broadcast(
            "enemy_added", f"{enemy.id} #{enemy.color:06X}")

        await self._broadcast('enemy_added', {
            'id': enemy.id,
            'color': enemy.color,
            'health': enemy.health,
            'source': enemy.source.dict(),
            'target': enemy.target.dict(),
            'start_time': enemy.start_time,
            'speed': enemy.speed
        })

    # An enemy has received damage
    async def enemy_damaged(self, enemy: Enemy, damage: int):
        self.logger.log_broadcast(
            "enemy_damaged", f"{enemy.id} is down to {damage} HP")

        await self._broadcast('enemy_damaged', {
            'id': enemy.id,
            'health': damage
        })

    # An enemy has been removed from the game
    async def enemy_removed(self, enemy: Enemy):
        self.logger.log_broadcast("enemy_removed", f"{enemy.id}")

        await self._broadcast('enemy_removed', enemy.id)

    # Player rotation has been updated
    async def player_rotation_updated(self, username: str, rotation: Vec3):
        self.logger.log_broadcast(
            "player_rotation_updated", f"{username} {rotation}", highlight=False)

        await self._broadcast('player_rotation_updated', {
            'username': username,
            'rotation': rotation.dict()
        })

    # Player score has been updated
    async def player_score_updated(self, username: str, score: int):
        self.logger.log_broadcast(
            "player_score_updated", f"{username} {score} pts")

        await self._broadcast('player_score_updated', {
            'username': username,
            'score': score
        })

    # Player health has been updated
    async def player_damaged(self, username: str, health: int):
        self.logger.log_broadcast(
            "player_damaged", f"{username} {health} HP")

        await self._broadcast('player_damaged', {
            'username': username,
            'health': health
        })

# Game class


class Game:
    # Global time of the game
    time: int
    # Game duration
    duration: int = 120_000  # 2 minutes

    # Map of enemies in the game
    enemies: dict[int, Enemy]
    # Players in the game
    players: dict[str, Player]

    # Logger for the game
    logger: Logger
    # Game broadcasts and events for communication with the clients
    broadcast: GameBroadcasts = GameBroadcasts()
    # Last enemy identifier
    last_enemy_id: int = 0

    # If the game is over
    is_over: bool = False

    @property
    def player_list(self): return list(self.players.values())
    @property
    def enemy_list(self): return list(self.enemies.values())

    # A game can only be created with a list of players
    def __init__(self, players: list[str], clients: list[web.WebSocketResponse]):
        # Create a game identifier for the logs, to differentiate the games
        self.logger = Logger(random.randint(1000, 9999))

        self.enemies = {}
        self.players = {}
        self.broadcast = GameBroadcasts(clients, self.logger)

        RADIUS = 4

        for (i, username) in enumerate(players):
            # The player should be created so that it lies on the circumference of a circle
            angle = i * 2 * math.pi / len(players)

            sina = RADIUS * math.sin(angle)
            cosa = RADIUS * math.cos(angle)

            position = Vec3(cosa, 0, sina)
            rotation = Vec3(0, angle + math.pi, 0)

            player = Player(username)
            player.position = position
            player.rotation = rotation
            self.players[username] = player

    async def start(self):
        """Starts the game and broadcasts the start of the game to all the players."""
        self.time = 0
        await self.broadcast.game_started(self.player_list)

    async def spawn_enemy(self):
        """Creates a new random enemy and adds it to the game."""
        # Decide who to target.
        player: Player = random.choice(self.player_list)

        # Decide the position of the enemy
        # TODO Use a more intelligent algorithm
        x = random.uniform(-4, 4)
        y = random.uniform(-3, 3)
        z = random.uniform(-4, 4)

        r = random.randint(96, 255) * 0x10000
        g = random.randint(96, 255) * 0x100
        b = random.randint(96, 255)
        color = r + g + b

        # Create the enemy
        enemy = Enemy(
            id=self.last_enemy_id,
            # TODO Define color and health based on some logic
            health=1,
            color=color,
            source=Vec3(x, y, z),
            target=player.position,
            start_time=self.time,
            speed=0.0001
        )
        self.last_enemy_id += 1

        # Add the enemy to the game
        self.enemies[enemy.id] = enemy
        await self.broadcast.enemy_added(enemy)

    async def update(self):
        """Update the game state and broadcast the changes to the clients."""
        if self.time >= self.duration:
            # Get the highest score
            highest_score = max(
                self.player_list, key=lambda player: player.score)
            # Get the players with the highest score
            winners = [
                player for player in self.player_list if player.score == highest_score.score]

            if len(winners) == 1:
                await self.broadcast.game_over(
                    f"{winners[0].username} has won with {winners[0].score} points!")
            else:
                await self.broadcast.game_over(
                    f"The game ended in a draw with {winners[0].score} points!")

            return True

        # Spawn a new enemy every second
        if self.time % 2000 == 0:
            await self.broadcast.time_sync(self.time)
            await self.spawn_enemy()

        # Compute the distance between each player and each enemy
        for player in self.player_list:
            for enemy in self.enemy_list:
                enemy_position = enemy.get_position(self.time)
                distance = player.position.distance(enemy_position)

                # If the player is close to the enemy, damage the player
                if distance < 0.5:
                    damage = enemy.health * 10
                    # if the player died, broadcast it.
                    if await self.player_take_damage(player, damage):
                        return True
                    # Remove the enemy
                    del self.enemies[enemy.id]
                    await self.broadcast.enemy_removed(enemy)

        self.time += 50
        # Update the logger time
        self.logger.time = self.time

        return False

    async def run(self):
        """Run the game loop."""
        try:
            await self.start()
            while True:
                stop = await self.update()
                if stop:
                    self.is_over = True
                    break
                await asyncio.sleep(0.05)
        except Exception as e:
            self.logger.log_error(
                f"Game terminated due to error: {e.__cause__}")

    async def player_take_damage(self, player: Player, damage: int) -> bool:
        """Applies damage to a player and broadcasts the change."""
        player.health -= damage

        if player.health <= 0:
            player.health = 0
            await self.broadcast.game_over(f"{player.username} has been defeated!")
            return True
        else:
            await self.broadcast.player_damaged(player.username, player.health)
            return False

    # Event handlers
    async def on_player_rotation_updated(self, username: str, rotation: Vec3):
        """Event handler for when a player has updated its rotation."""
        # print(f"\nEvent: player_rotated {username} moved to {rotation}")
        player = self.players[username]
        player.rotation.copy(rotation)

        await self.broadcast.player_rotation_updated(player.username, rotation)

    async def on_enemy_shot(self, username: str, enemy_id: int):
        """Event handler for when a player has shot an enemy."""
        self.logger.log(f"{username} shot {enemy_id}")
        player = self.players[username]
        if enemy_id not in self.enemies:
            self.logger.log_error(f"Enemy {enemy_id} does not exist!")
            return
        enemy = self.enemies[enemy_id]

        # Decrease the health of the enemy
        enemy.health -= 1
        if enemy.health <= 0:
            await self.broadcast.enemy_removed(enemy)
            del self.enemies[enemy.id]
            player.score += 10
            await self.broadcast.player_score_updated(player.username, player.score)
        else:
            await self.broadcast.enemy_damaged(enemy, enemy.health)

    async def on_player_disconnect(self, username: str):
        """Event handler for when a player has disconnected."""
        self.logger.log(f"{username} has disconnected")
        if username in self.players:
            del self.players[username]

        # If the game is already over, someone disconnecting is not a big deal
        if self.is_over:
            return
        await self.broadcast.game_over(f"{username} has disconnected.")

    async def on_player_ready(self, username: str):
        """Event handler for when a player is ready to start the game."""
        self.logger.log(f"{username} is ready to start the game")
        if username in self.players:
            player = self.players[username]
            player.ready = True

        if all(player.ready for player in self.players.values()):
            asyncio.create_task(self.run())
