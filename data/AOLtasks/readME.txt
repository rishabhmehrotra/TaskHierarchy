Gabriele Tolomei’s Home

Feb 14 2011

AOL: ground-truth of tasks

Published by Gabriele

This page allows you to download a set of Web tasks manually-extracted from a sample data set of the 2006 AOL query log. Each Web task is in turn composed of a set of queries that are related to the same intent.

The following aol-task-ground-truth.tar.gz file contains:

—> all-tasks.txt: a single, preprocessed, cleaned, and tab-delimited file representing the whole tasks. In particular, each row contains: the anonymous AOL userID, the timesessionID (which is obtained by splitting the original query stream as long as the time gap between two consecutive queries is greater than 26 minutes), the taskID inside the specific timesessionID, the original queryID, and finally the querystring itself.

—> a set of directories corresponding to each anonymized AOL userID. Inside each directory there are as many files as the tasks extracted for that userID, named as: userID-timesessionID-taskID.txt. Each file contains two tab-delimited columns, namely the original queryID and the querystring itself, which represent a particular task.