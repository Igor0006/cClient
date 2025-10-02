# cClient
A lightweight console client for the cChat messanger without using any additional TUI frameworks. 
### Tech Stack
- **Java / Spring**
  - Spring Context, Spring Messaging (STOMP over WebSocket)
  - Spring WebSocket STOMP Client
- **Maven**
### Commands
- **/login** or **/register** — authenticate or create a new account  
- **/help** — show description of all available commands  
- **/conversations** — list your conversations  
- **/open <conversation_id>** — open a dialog by its ID  
- **/send** — send a message in the currently opened chat  
- **/addContact <userName>** — add a user with the specified username to your conversations  
- **/createGroup <GroupName> <userName1> <userName2> ...** — create a group and add listed users into it  
- **/exit** — exit the app  