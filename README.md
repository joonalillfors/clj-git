# clj-git

/api/languages/{github username}

Returns JSON with stats about how much the user has used different programming languages their Github repositories

## Prerequisites

Setting up .lein-env file with github client token is required to access Github API for repository data.

.lein-env: { :client-token "YOUR TOKEN HERE" }

You will need [Leiningen][] 2.0.0 or above installed.

[leiningen]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run:

    lein ring server

## License

Copyright © 2019 FIXME
