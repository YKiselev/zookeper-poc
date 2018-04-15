-----------------------------
ZooKeeper
-----------------------------

https://zookeeper.apache.org/doc/current/zookeeperOver.html

1) Data Model
- Hierarchical name space (much like a distributed file system)
- Each node can have data value as well as children
- Any unicode character can be used in a path subject to the following constraints:
    The null character (\u0000) cannot be part of a path name. (This causes problems with the C binding.)
    The following characters can't be used because they don't display well, or render in confusing ways: \u0001 - \u0019 and \u007F - \u009F.
    The following characters are not allowed: \ud800 -uF8FFF, \uFFF0 - uFFFF.
    The "." character can be used as part of another name, but "." and ".." cannot alone be used to indicate a node
    along a path, because ZooKeeper doesn't use relative paths. The following would be invalid: "/a/b/./c" or "/a/b/../c".
    The token "zookeeper" is reserved.

2) ZNode
- Every node in a ZooKeeper tree is referred to as a znode.
- Watches
    Clients can set watches on znodes. Changes to that znode trigger the watch and then clear the watch.
    When a watch triggers, ZooKeeper sends the client a notification.
- Data Access
    The data stored at each znode is read and written atomically. Each node has an Access Control List (ACL) that restricts who can do what.
    Not designed to be a general database or large object store.
    Node payload should be measured in kilobytes.
    Have limit of 1M of data but typical value should be much less.
- Ephemeral Nodes
    These znodes exists as long as the session that created the znode is active. When the session ends the znode is deleted.
    Because of this behavior ephemeral znodes are not allowed to have children.
- Sequence Nodes
    When creating a znode you can also request that ZooKeeper append a monotonically increasing counter to the end of path.
    This counter is unique to the parent znode. The counter has a format of %010d -- that is 10 digits with 0 (zero) padding
    (the counter is formatted in this way to simplify sorting), i.e. "<path>0000000001".

3) Time in ZooKeeper
- Zxid
    Every change receives a stamp (ZooKeeper Transaction Id).
    This exposes the total ordering of all changes to ZooKeeper. If zxid1 < zxid2 then zxid1 happened before zxid2.

- Version numbers
    Every change to a node will cause an increase to one of the version numbers of that node. The three version numbers
    are version (number of changes to the data of a znode),
    cversion (number of changes to the children of a znode), and
    aversion (number of changes to the ACL of a znode).

- Ticks
    Servers use ticks to define timing of events such as status uploads, session timeouts, connection timeouts between peers, etc.
    The tick time is only indirectly exposed through the minimum session timeout (2 times the tick time);

- Real time
    ZooKeeper doesn't use real time/clock time, at all except to put timestamps into the stat structure on znode creation and znode modification.

4) ZooKeeper Stat Structure
    example:
        Stat stat = zk.exists("/a/b/c", false)

    The Stat structure for each znode in ZooKeeper is made up of the following fields:

    czxid
    The zxid of the change that caused this znode to be created.

    mzxid
    The zxid of the change that last modified this znode.

    pzxid
    The zxid of the change that last modified children of this znode.

    ctime
    The time in milliseconds from epoch when this znode was created.

    mtime
    The time in milliseconds from epoch when this znode was last modified.

    version
    The number of changes to the data of this znode.

    cversion
    The number of changes to the children of this znode.

    aversion
    The number of changes to the ACL of this znode.

    ephemeralOwner
    The session id of the owner of this znode if the znode is an ephemeral node. If it is not an ephemeral node, it will be zero.

    dataLength
    The length of the data field of this znode.

    numChildren
    The number of children of this znode.

4) Consistency Guarantees
- High performance, scalable service.
- Reads/writes designed to be fast, though reads are faster.
- Sequential Consistency
    Updates from a client will be applied in the order that they were sent.
- Atomicity
    Updates either succeed or fail - there are no partial results.
- Single System Image
    A client will see the same view of the service regardless of the server that it connects to.
- Reliability
    Once an update has been applied, it will persist from that time forward until a client overwrites the update.
    This guarantee has two corollaries:
        If a client gets a successful return code, the update will have been applied. On some failures (communication errors,
         timeouts, etc) the client will not know if the update has applied or not. Guarantee is only present with successful return codes.
    Any updates that are seen by the client, through a read request or successful update, will never be rolled back when recovering from server failures.
- Timeliness
    The clients view of the system is guaranteed to be up-to-date within a certain time bound (on the order of tens of seconds).
    Either system changes will be seen by a client within this bound, or the client will detect a service outage.

- There is NO "Simultaneously Consistent Cross-Client Views"
    ZooKeeper does not guarantee that at every instance in time, two different clients will have identical views of ZooKeeper data.

5) Simple API
    One of the design goals of ZooKeeper is provide a very simple programming interface. As a result, it supports only these operations:

create
creates a node at a location in the tree

delete
deletes a node

exists
tests if a node exists at a location

get data
reads the data from a node

set data
writes data to a node

get children
retrieves a list of children of a node

sync
waits for data to be propagated

-----------------------------
Curator -  A ZooKeeper Keeper
-----------------------------

https://curator.apache.org/index.html

org.apache.curator:curator-framework
    The Curator Framework high level API. This is built on top of the client and should pull it in automatically.
org.apache.curator:curator-x-discovery
    A Service Discovery implementation built on the Curator Framework.

- Fluent API/builders everywhere
- Most of the objects are Closeable and need to be started (using start method).

RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3)
CuratorFramework client = CuratorFrameworkFactory.newClient(zookeeperConnectionString, retryPolicy);
client.start();
client.create().forPath("/my/path", myData)