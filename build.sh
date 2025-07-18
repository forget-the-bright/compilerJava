   git pull --rebase
   docker rm -f compiler-app
   docker rmi compiler-java-app:latest
   mvn clean package  -Dfile.encoding=UTF-8 -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Dmaven.wagon.http.ssl.ignore.validity.dates=true -U -Dmaven.test.skip=true
   docker build -t compiler-java-app .
   docker run -d -p 801:80 --name compiler-app compiler-java-app