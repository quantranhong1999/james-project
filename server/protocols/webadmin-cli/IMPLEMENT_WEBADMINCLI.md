# Architecture Decision Record for new James CLI

## Why implement a CLI based on webadmin API

James servers offer a command-line interface in order to interact with the server. However, it relies on the JMX protocol, which is known to be insecure.

Webadmin APIs use HTTP protocol, which is more secure than JMS protocol to interact with James servers.

Webadmin command-line interface is an upcoming replacement for the outdated, security-vulnerable JMX command-line interface.  

## Context
We decided some points about new James CLI architecture:

* What libraries will we use? 

  * http client: Feign library

  * CLI: Picocli library

* How will we limit breaking changes this new CLI will cause?

  * Work on a wrapper to adapt the old CLI API.

* Where will we locate this cli code?

  * server/protocols/webadmin-cli

* General syntax to run the command line

```   
$ java -jar james_cli.jar [OPTION] ENTITY ACTION {ARGUMENT}
```
where

    OPTION: optional parameter when running the command line,
  
    ENTITY: represents the entity to perform action on,
  
    ACTION: name of the action to perform,
  
    ARGUMENT: arguments needed for the action.




## Consequences

It aims at providing a more modern and more secure CLI, also bringing compatibility ability with old CLI.