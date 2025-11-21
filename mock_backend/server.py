from flask import Flask, request, jsonify

app = Flask(__name__)

@app.route("/api/metrics", methods=["POST"])
def metrics():
    print("Backend received:", request.json)
    return jsonify({"status":"ok"}), 200

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8080)


