docker login

docker tag ai-translate-backend:latest zhouyuan932446/ai-translate-backend:latest

docker push zhouyuan932446/ai-translate-backend:latest

docker run -d \
  -p 8124:8124 \
  --name ai-translate-backend \
  ai-translate-backend:latest