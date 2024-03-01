import asyncio
from aiohttp import web, WSMessage, WSMsgType

from game import Game, Vec3, Logger

L = Logger()


class GamesServer:
    # The web application
    app: web.Application
    # Players who are logged in as sockets
    players: dict[str, web.WebSocketResponse] = {}

    def __init__(self, port: int):
        self.app = web.Application()
        self.app.router.add_get('/', self.websocket_handler)
        L.log("Server starting on port 8080")
        L.log("Press Ctrl+C to stop the server...")
        web.run_app(self.app, port=port, print=None)

    open_games: dict[str, Game] = {}

    async def websocket_handler(self, request: web.Request):
        """The handler for the websocket connection. This is where the game interface logic will be implemented."""
        ws = web.WebSocketResponse()
        await ws.prepare(request)

        # Send a welcome message to the user
        await self.send_message_to_anon(ws, 'ask_name', 'Welcome to the game server! Choose a name!')

        username = ''
        while username == '':
            # Wait for the user to send a username
            # FIXME TypeError: Received message 8:0 is not WSMsgType.TEXT
            username = await ws.receive_str()

            # Make sure the username is not already taken
            if username in self.players:
                await self.send_message_to_anon(ws, 'ask_name', 'Username is already taken')
                username = ''

        # Add the user to the list of players
        self.players[username] = ws
        # Send the user a message that they are ready to play
        await self.send_message(username, 'ready', f'You are playing with the name \'{username}\'!')

        L.log(f"User {username} has connected")

        self.match_making_queue.append(username)
        self.waiting_for_match.add(username)
        # Start the match making loop
        while username in self.waiting_for_match:
            await self.match_making(username)
            await asyncio.sleep(1)

        game = self.open_games[username]
        async for msg in ws:
            # Decipher the message from JSON
            try:
                msg = msg.json()
                msg_type = msg['type']
                msg_data = msg['data']

                if msg_type == 'player_ready':
                    await game.on_player_ready(username)
                elif msg_type == 'player_rotation_updated':
                    rotation = Vec3(
                        msg_data['x'], msg_data['y'], msg_data['z'])
                    await game.on_player_rotation_updated(username, rotation)
                elif msg_type == 'enemy_shot':
                    id = msg_data['id']
                    await game.on_enemy_shot(username, id)
            except Exception as e:
                L.log_error(
                    f"Received an invalid message from {username}: {msg}")
                L.log_error(e)
                continue

        # Once the message loop is over, the player has disconnected
        L.log(f"User {username} has disconnected")
        del self.players[username]
        del self.open_games[username]
        # This should not be necessary, but just in case
        self.waiting_for_match.discard(username)

        # If the game is still open, send the disconnection event
        # to everyone else who is connected
        await game.on_player_disconnect(username)

        return ws

    async def send_message_to_anon(self, ws: web.WebSocketResponse, msg_type: str, msg_data: dict | str | int):
        """Send a message to a socket."""
        await ws.send_json({
            'type': msg_type,
            'data': msg_data
        })

    async def send_message(self, username: str, msg_type: str, msg_data: dict | str | int):
        """Send a message to a player."""
        if not username in self.players:
            raise Exception(f"Player {username} is not connected")

        try:
            await self.send_message_to_anon(self.players[username], msg_type, msg_data)
        except Exception as e:
            L.log_error(f"Failed to send a message to {username}: {e}")
            # Assume the player has disconnected
            del self.players[username]

            raise e

    # List of players who are looking for a match
    waiting_for_match: set[str] = set()
    match_making_queue: list[str] = []
    match_making_semaphore = asyncio.Semaphore(1)

    async def match_making(self, username: str):
        """The loop that matches players together."""
        ws = self.players[username]
        match = (None, None)

        async with self.match_making_semaphore:
            # If there are enough players in the queue, match them together
            if len(self.match_making_queue) >= 2:
                player1 = self.match_making_queue.pop(0)
                player2 = self.match_making_queue.pop(0)
                # Remove both players from the waiting list
                self.waiting_for_match.remove(player1)
                self.waiting_for_match.remove(player2)

                match = (player1, player2)

        if match[0] != None:
            player1, player2 = match
            socket1 = self.players[player1]
            socket2 = self.players[player2]

            # Send the players a message that they are matched together
            try:
                await self.send_message(player1, 'matched', player2)
                await self.send_message(player2, 'matched', player1)

                # Create a new game
                game = Game([player1, player2], [socket1, socket2])

                self.open_games[player1] = game
                self.open_games[player2] = game
                L.log(f"New game created with `{player1}` and `{player2}`")
                L.log("Waiting for both players to be ready...")
            except Exception:
                async with self.match_making_semaphore:
                    L.log_error(f"Match failed because someone disconnected")
                    # Readd a player to the queue only if it is still connected
                    if player1 in self.players:
                        self.match_making_queue.append(player1)
                        self.waiting_for_match.add(player1)
                        L.log(
                            f"Re-added {player1} because it is still connected")
                    if player2 in self.players:
                        self.match_making_queue.append(player2)
                        self.waiting_for_match.add(player2)
                        L.log(
                            f"Re-added {player2} because it is still connected")
        else:
            # Send the player a message that they are waiting for a match
            await self.send_message_to_anon(ws, 'waiting', 'Waiting for a match...')


if __name__ == "__main__":
    GamesServer(8080)
