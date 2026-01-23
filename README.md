# SurrealDB unofficial RPC client

A lightweight RPC client because I don't want to include tens of megabytes
worth of native libraries in a Mindustry plugin.

## URLs

SurrealRPC uses the following URL format:

`ws(s)://(host)/(path)/(namespace)/(database)`

In `path`, `/rpc` must be ommited.

## Authorization

**Root**

To log in with username and password, add those in the authorization part of the URL:

`ws://username:password@localhost/ns/db`

**Token**

Token must be specified in authorization part of the URL:

`ws://qwertytuyiop@localhost/ns/db`

*Those are all the schemes that are supported right now*
