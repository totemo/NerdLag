NerdLag
=======
Tools for detecting various kinds of lag.


Commands
--------
Note that, unless otherwise noted, plugin settings do not persist across restarts.  The plugin defaults to a safe, passive state.


```/nerdlag event watch (<plugin> | all) [<thresh_nanos>]```
 * Start watching the duration of event handlers for the specified plugin, or all plugins.
 * If an integer argument <thresh_nanos> is given, then event handlers that take longer than that many nanoseconds to execute are reported.
 * Maximum event durations are reported by `/nerdlag event report` (not yet implemented).  Until that command is implemented, it is advisable to always specify a reporting threshold, since there is no other way to see results.
 
```/nerdlag event unwatch (<plugin> | all)</li>```
 * Stop watching the duration of event handlers for the specified plugin, or all plugins.

```/nerdlag event notify (on|off)```
 * Control whether the player receives in-game notifications of reports of events that exceed their duration threshold.

```/nerdlag event log (on|off)```
 * Control whether reports of events that exceed their duration threshold will be written to the server log.
 * By default, reports are not logged.


Permissions
-----------
To use nerdlag, you must have the nerdlag.admin permission.

