"""Arranque de FlightDeck: servidor web local + navegador.

Uso: python -m flightDeck [--port 8765] [--no-browser]
"""

import argparse
import threading
import webbrowser

from .server import make_server


def main():
    parser = argparse.ArgumentParser(prog="flightDeck")
    parser.add_argument("--port", type=int, default=8765)
    parser.add_argument("--no-browser", action="store_true", help="No abrir el navegador")
    opts = parser.parse_args()

    server = make_server(opts.port)
    url = f"http://127.0.0.1:{opts.port}/"
    print(f"FlightDeck en {url} (Ctrl+C para salir)")
    if not opts.no_browser:
        threading.Timer(0.5, webbrowser.open, args=(url,)).start()
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
