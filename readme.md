# Remote Android Forth REPL 
Uh Oh, looks like I need to add some semblance of documentation before the Forth2020 meeting


PC bluetooth host takes input from the user (on the host machine), sends them over bluetooth to the Android Client, waits for the Forth interpreter on the client to process the Forth command, and displays the reponse by the client

Reflection is supported, so you can see and call Java methods and classes from Forth without special interfaces.

Features
- BT server
- BT client
- No modification to existing java codebase
- Forth interpreter supports compilation and branch and xts and all the good stuff but is built with Java in mind
- Multi tasking
- 
