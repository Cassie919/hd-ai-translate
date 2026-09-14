docker login
#本地：
docker build -t ai-translate-backend:latest .

#远程：
docker build --platform linux/amd64 -t ai-translate-backend:latest .

docker tag ai-translate-backend:latest zhouyuan932466/ai-translate-backend:latest

docker push zhouyuan932446/ai-translate-backend:latest

docker run -d \
  -p 8124:8124 \
  --name ai-translate-backend \
  -e SPRING_PROFILES_ACTIVE=local \
  ai-translate-backend:latest