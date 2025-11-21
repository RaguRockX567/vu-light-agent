from http.server import HTTPServer, BaseHTTPRequestHandler
import json

class MockHandler(BaseHTTPRequestHandler):
    def do_POST(self):
        try:
            content_len = int(self.headers.get('Content-Length', 0))
            if content_len > 0:
                post_body = self.rfile.read(content_len).decode('utf-8')
                print(f"\nReceived metrics: {post_body}")
            
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            self.wfile.write(json.dumps({"status": "ok"}).encode())
        except Exception as e:
            print(f"Error: {e}")
            self.send_error(500, str(e))

if __name__ == "__main__":
    server = HTTPServer(('localhost', 8080), MockHandler)
    print("Mock server started on http://localhost:8080")
    server.serve_forever()