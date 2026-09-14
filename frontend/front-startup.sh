#本地
docker build -t ai-translate-frontend:latest .

#远程
docker build --platform linux/amd64 -t ai-translate-frontend:latest .

docker tag ai-translate-frontend:latest zhouyuan932466/ai-translate-frontend:latest

docker push zhouyuan932466/ai-translate-frontend:latest

docker run -d \
  -p 3001:80 \
  --name ai-translate-frontend \
  ai-translate-frontend:latest