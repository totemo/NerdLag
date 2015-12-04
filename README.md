NerdLag
=======
Tools for detecting various kinds of lag.


Commands
--------

```/nerdlag event watch (<plugin> | all) [<thresh_nanos>]```
 * Start watching the duration of event handlers for the specified plugin, or all plugins.
 * If an integer argument <thresh_nanos> is given, then event handlers that take longer than that many nanoseconds to execute are reported.
 * Maximum event durations are reported by `/nerdlag event report` (not yet implemented).  Until that command is implemented, it is advisable to always specify a reporting threshold, since there is no other way to see results.
 
```/nerdlag event unwatch (<plugin> | all)</li>```
 * Stop watching the duration of event handlers for the specified plugin, or all plugins.


```/nerdlag event subscribe```
 * Subscribe to in-game notifications of reports of events that exceed their duration threshold.
 * Reports will always be written to the server log, regardless of whether you subscribe to in-game notifications.

```/nerdlag event unsubscribe```
 * Unsubscribe from in-game notifications of reports of events that exceed their duration threshold.


Permissions
-----------
To use nerdlag, you must have the nerdlag.admin permission.

