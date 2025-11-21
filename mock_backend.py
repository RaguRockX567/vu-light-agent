from flask import Flask, request, jsonify
import time

app = Flask(__name__)

@app.route("/api/metrics", methods=["POST"])
def receive_metrics():
    metrics = request.json
    print(f"\n📥 {time.strftime('%H:%M:%S')} Received metrics batch size={len(metrics)}")
    for metric in metrics:
        print(f"  - {metric['name']}: {metric['value']}")
    return jsonify({"status": "ok", "received": len(metrics)})

if __name__ == "__main__":
    print("🚀 Mock backend server starting on http://localhost:8080")
    app.run(host="0.0.0.0", port=8080)