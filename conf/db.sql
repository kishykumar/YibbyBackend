--database settings
alter database DATETIMEFORMAT yyyy-MM-dd'T'HH:mm:ss.SSSZ
alter database custom useLightweightEdges=false
alter database custom useClassForEdgeLabel=false
alter database custom useClassForVertexLabel=true
alter database custom useVertexFieldsForEdgeLabels=true

--classes


--Node
create class _BB_NodeVertex extends V;

--Node class should be abstract but we cannot declare it as abstrat due the index on the id field
create class _BB_Node  extends ORestricted;
create property _BB_NodeVertex._node link _BB_Node;
create property _BB_Node._creation_date datetime;
create property _BB_Node._links link _BB_NodeVertex;
create property _BB_Node.id String;
create property _BB_Node._author String;


--user
create class _BB_User extends _BB_Node;
create class _BB_UserAttributes extends ORestricted;
create property _BB_User.visibleByAnonymousUsers link _BB_UserAttributes;
create property _BB_User.visibleByRegisteredUsers link _BB_UserAttributes;
create property _BB_User.visibleByFriend link _BB_UserAttributes;
create property _BB_User.visibleByTheUser link _BB_UserAttributes;
create property _BB_User._audit embedded;
create property _BB_User.user link ouser;
-- issue 447 - Restrict signup to 1 account per email
create property _bb_userattributes.email string;
--the enforcement of the uniqueness of registration email is performed by the code due the fact that there could be email fields in other profile sections
create index _bb_userattributes.email notunique;

--delete OrientDB default users
delete from OUser where name in ['reader','writer'];

--admin user
insert into _BB_User set user = (select from ouser where name='admin'), _links = (insert into _BB_NodeVertex set _node=null), _creation_date = sysdate(), signUpDate = sysdate();
update _BB_NodeVertex set _node=(select from _BB_User where user.name='admin');
 
--reset pwd
create class _BB_ResetPwd;

--users' constraints
alter property _BB_NodeVertex._node mandatory=true;
alter property _BB_NodeVertex._node notnull=true;
alter property _BB_Node._creation_date mandatory=true;
alter property _BB_Node._creation_date notnull=true;
alter property _BB_User.user mandatory=true;
alter property _BB_User.user notnull=true;
alter property _BB_Node._links mandatory=true;
alter property _BB_Node._links notnull=true;

--object storage
create class _BB_Collection extends _BB_Node;
create property _BB_Collection.name String;
alter property _BB_Collection.name mandatory=true;
alter property _BB_Collection.name notnull=true;

--files
create class _BB_File extends _BB_Node;
create property _BB_File.fileName String;
alter property _BB_File.fileName mandatory=true;
alter property _BB_File.fileName notnull=true;
create property _BB_File.contentType String;
alter property _BB_File.contentType mandatory=true;
alter property _BB_File.contentType notnull=true;
create property _BB_File.contentLength long;
alter property _BB_File.contentLength mandatory=true;
alter property _BB_File.contentLength notnull=true;
create property _BB_File.file link;
alter property _BB_File.file mandatory=true;
alter property _BB_File.file notnull=true;

create class _BB_File_Content;
create property _BB_File_Content.content String;
create index _BB_File_Content.content.key FULLTEXT_HASH_INDEX;


--Assets
create class _BB_Asset extends _BB_Node;
create class _BB_FileAsset extends _BB_Asset;
create property _BB_Asset.name String;
alter property _BB_Asset.name mandatory=true;
alter property _BB_Asset.name notnull=true;
create property _BB_FileAsset.fileName String;
alter property _BB_FileAsset.fileName mandatory=true;
alter property _BB_FileAsset.fileName notnull=true;
create property _BB_FileAsset.contentType String;
alter property _BB_FileAsset.contentType mandatory=true;
alter property _BB_FileAsset.contentType notnull=true;
create property _BB_FileAsset.contentLength long;
alter property _BB_FileAsset.contentLength mandatory=true;
alter property _BB_FileAsset.contentLength notnull=true;
create property _BB_FileAsset.file link;
alter property _BB_FileAsset.file mandatory=true;
alter property _BB_FileAsset.file notnull=true;

--permissions
create class _BB_Permissions;
create property _BB_Permissions.tag String;
create property _BB_Permissions.enabled boolean;
alter property _BB_Permissions.tag mandatory=true;
alter property _BB_Permissions.tag notnull=true;
alter property _BB_Permissions.enabled mandatory=true;
alter property _BB_Permissions.enabled notnull=true;

create property orole.isrole boolean
update orole set isrole=true

--indices

alter property ouser.name collate ci;
create index _BB_Collection.name UNIQUE_HASH_INDEX;
create index _BB_asset.name unique;
create index _BB_Node.id UNIQUE_HASH_INDEX;
create index _BB_Permissions.tag UNIQUE_HASH_INDEX;
---bug on OrientDB index? (our issue #412) We have to define a "new" index to avoid class scan when looking for a username:

create index _bb_user.user.name unique;
create index _bb_node._author notunique;
create index _bb_node._creation_date notunique;

--configuration class
create class _BB_Index;
create property _BB_Index.key String;
alter property _BB_Index.key mandatory=true;
alter property _BB_Index.key notnull=true;
create index _BB_Index.key unique;

--LINKS
create property E.id String;
alter property E.id notnull=true;
create index E.id UNIQUE_HASH_INDEX;

--Scripts
create class _BB_Script;
create property _BB_Script.name String;
alter property _BB_Script.name mandatory=true;
alter property _BB_Script.name notnull=true;
create property _BB_Script.code embeddedlist string;
alter property _BB_Script.code mandatory=true;
alter property _BB_Script.code notnull=true;
create property _BB_Script.lang string;
alter property _BB_Script.lang mandatory=true;
alter property _BB_Script.lang notnull=true;
create property _BB_Script.library boolean;
alter property _BB_Script.library mandatory=true;
alter property _BB_Script.library notnull=true;
create property _BB_Script.active boolean;
alter property _BB_Script.active mandatory=true;
alter property _BB_Script.active notnull=true;
create property _BB_Script._storage embedded;
create property _BB_Script._creation_date datetime;
create property _BB_Script._invalid boolean;
alter property _BB_Script._invalid mandatory=true;
alter property _BB_Script._invalid notnull=true;
create index _BB_Script.name unique;





--Bid
create class _BB_Bid extends _BB_Node;

create property _BB_Bid.bidPrice DOUBLE;
alter property _BB_Bid.bidPrice mandatory=true;
alter property _BB_Bid.bidPrice notnull=true;

create property _BB_Bid.pickupLat DOUBLE;
alter property _BB_Bid.pickupLat mandatory=true;
alter property _BB_Bid.pickupLat notnull=true;

create property _BB_Bid.pickupLong DOUBLE;
alter property _BB_Bid.pickupLong mandatory=true;
alter property _BB_Bid.pickupLong notnull=true;

create property _BB_Bid.pickupLoc String;
alter property _BB_Bid.pickupLoc mandatory=true;
alter property _BB_Bid.pickupLoc notnull=true;

create property _BB_Bid.dropoffLat DOUBLE;
alter property _BB_Bid.dropoffLat mandatory=true;
alter property _BB_Bid.dropoffLat notnull=true;

create property _BB_Bid.dropoffLong DOUBLE;
alter property _BB_Bid.dropoffLong mandatory=true;
alter property _BB_Bid.dropoffLong notnull=true;

create property _BB_Bid.dropoffLoc String;
alter property _BB_Bid.dropoffLoc mandatory=true;
alter property _BB_Bid.dropoffLoc notnull=true;

create property _BB_Bid.driversList LINKLIST;
create property _BB_Bid.declineDriversList LINKLIST;
create property _BB_Bid.acceptDriversList LINKLIST;

create property _BB_Bid.finalDriver LINK;
alter property _BB_Bid.finalDriver notnull=true;

--driver
create class _BB_Driver extends _BB_Node;
create class _BB_DriverAttributes extends ORestricted;
create property _BB_Driver.visibleByAnonymousDrivers link _BB_DriverAttributes;
create property _BB_Driver.visibleByRegisteredDrivers link _BB_DriverAttributes;
create property _BB_Driver.visibleByFriend link _BB_DriverAttributes;
create property _BB_Driver.visibleByTheDriver link _BB_DriverAttributes;

create property _BB_Driver.active BOOLEAN;

create property _BB_Driver._audit embedded;
create property _BB_Driver.user link ouser;
alter property _BB_Driver.user mandatory=true;
alter property _BB_Driver.user notnull=true;


-- issue 447 - Restrict signup to 1 account per email
create property _bb_driverattributes.email string;
--the enforcement of the uniqueness of registration email is performed by the code due the fact that there could be email fields in other profile sections
create index _bb_driverattributes.email notunique;
create index _bb_driver.user.name unique

--ride
create class _BB_Ride extends _BB_Node;

create property _BB_Ride.rider link;
alter property _BB_Ride.rider mandatory=true;
alter property _BB_Ride.rider notnull=true;

create property _BB_Ride.driver link;
alter property _BB_Ride.driver mandatory=true;
alter property _BB_Ride.driver notnull=true;

create property _BB_Ride.duration Integer;

create property _BB_Ride.pickupTime datetime;

create property _BB_Ride.dropoffTime datetime;

create property _BB_Ride.driverEnRouteTime datetime;

create property _BB_Ride.finalPickupLat DOUBLE;

create property _BB_Ride.finalPickupLong DOUBLE;

create property _BB_Ride.finalPickupLoc String;

create property _BB_Ride.finalDropoffLat DOUBLE;

create property _BB_Ride.finalDropoffLong DOUBLE;

create property _BB_Ride.finalDropoffLoc String;

create property _BB_Ride.tip DOUBLE;

create property _BB_Ride.ridePrice DOUBLE;

create property _BB_Ride.driverEarnedAmount DOUBLE;

create property _BB_Ride.creditCardFee DOUBLE;

create property _BB_Ride.driverStartLat DOUBLE;

create property _BB_Ride.driverStartLong DOUBLE;

create property _BB_Ride.driverStartLoc String;

--vehicle
create class _BB_Vehicle extends _BB_Node;

create property _BB_Vehicle.inspectionFormPicture link;
alter property _BB_Vehicle.inspectionFormPicture mandatory=true;
alter property _BB_Vehicle.inspectionFormPicture notnull=true;

create property _BB_Vehicle.exteriorColor String;
alter property _BB_Vehicle.exteriorColor mandatory=true;
alter property _BB_Vehicle.exteriorColor notnull=true;

create property _BB_Vehicle.licensePlate String;
alter property _BB_Vehicle.licensePlate mandatory=true;
alter property _BB_Vehicle.licensePlate notnull=true;

create property _BB_Vehicle.make String;
alter property _BB_Vehicle.make mandatory=true;
alter property _BB_Vehicle.make notnull=true;

create property _BB_Vehicle.model String;
alter property _BB_Vehicle.model mandatory=true;
alter property _BB_Vehicle.model notnull=true;

create property _BB_Vehicle.year Integer;
alter property _BB_Vehicle.year mandatory=true;
alter property _BB_Vehicle.year notnull=true;

create property _BB_Vehicle.capacity Integer;
alter property _BB_Vehicle.capacity mandatory=true;
alter property _BB_Vehicle.capacity notnull=true;

--daily stats
create class _BB_DailyStats extends _BB_Node;

create property _BB_DailyStats.username String;
alter property _BB_DailyStats.username mandatory=true;
alter property _BB_DailyStats.username notnull=true;

create property _BB_DailyStats.collectionDate date;
alter property _BB_DailyStats.collectionDate mandatory=true;
alter property _BB_DailyStats.collectionDate notnull=true;

create property _BB_DailyStats.onlineTime Integer;
create property _BB_DailyStats.earning DOUBLE;
create property _BB_DailyStats.totalTrips Integer;

--weekly stats
create class _BB_WeeklyStats extends _BB_Node;

create property _BB_WeeklyStats.username String;
alter property _BB_WeeklyStats.username mandatory=true;
alter property _BB_WeeklyStats.username notnull=true;

create property _BB_WeeklyStats.collectionStartDate date;
alter property _BB_WeeklyStats.collectionStartDate mandatory=true;
alter property _BB_WeeklyStats.collectionStartDate notnull=true;

create property _BB_WeeklyStats.collectionEndDate date;
alter property _BB_WeeklyStats.collectionEndDate mandatory=true;
alter property _BB_WeeklyStats.collectionEndDate notnull=true;

create property _BB_WeeklyStats.onlineTime Integer;
create property _BB_WeeklyStats.earning DOUBLE;
create property _BB_WeeklyStats.paidAmount DOUBLE;
create property _BB_WeeklyStats.totalTrips Integer;

--payment stats
create class _BB_PaymentStats extends _BB_Node;

create property _BB_PaymentStats.collectionDate date;
alter property _BB_PaymentStats.collectionDate mandatory=true;
alter property _BB_PaymentStats.collectionDate notnull=true;

create property _BB_PaymentStats.status Integer;
