# ActivityMonitor
Includes an activity monitor REST Client and the REST API. 

## Client
Client for ActivityMonitor in Python. It has two features.
- **Window Watcher:** Keeps track of currently active application and the title of its window every second. Collets them in a JSON with additional information and send to server.
- **Screen Watcher:** Captures entire screen as PNG image and converts it to base64-encoded string. Puts base64-encoded string  in a JSON with additional information and send to server.

## Server
Server for ActivityMonitor. REST API built with Micronaut and written in Java. It has two endpoints. 
- **Active Window Endpoint:** Gets JSON from Window Watcher client and puts it into Elasticsearch. There is also **Kibana** sits on top of the Elasticsearch. It creates instant visualizations from stored JSONs in Elasticsearch.
- **Screenshot Endpoint:** Gets base64-encoded image string from Screen Watcher and decodes it. Creates a unique name for image and save it to a folder.

## How to build client ?
- Run "pyinstaller --onefile run_app.py" in client folder on Windows. This command will create dist/run_app.exe.
- Download and install Inno Setup to your computer. (https://jrsoftware.org/isdl.php)
- Open "WatcherClient.iss" with Inno Setup and compile. This will create dist/WatcherClient.exe.
- Run WatcherClient.exe to install WatcherClient to any windows computer. When installation is finished, WatcherClient will run automatically at startup.

## How to build Server ?
- Make sure docker-compose is installed on your computer.
- Run "docker-compose up" in server folder. This command will create docker containers for server to run.
