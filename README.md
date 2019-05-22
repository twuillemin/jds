# JSON Data Server

## Introduction

This project offers a complete backend allowing for multiple clients to discover and retrieve data stored in a 
PostgreSQL database at JSON format.

The software offers the following functions:

 * Multi-tenants: Multiple organizations can manage their own private repositories
 * User enrolment and security (JWT tokens)
 * Definition of JSON format by SQL queries for repository administrators
 * CRUD functions for repositories clients
 * Support of external authentication (google accounts)
 * Localization of error messages
 
Although the software can perfectly be used in stand alone mode, it is also architectured to be easily extendable or
adaptable. 

## Dependencies

The only dependencies is the usage of a PostgreSQL server.

## Version


| version | Description |
| --- | --- |
|v0.0.2-snapshot | Current development version. Mongo fully removed in favor of PostgresSQL. May work. Need more tests  |
|v0.0.1 | Original version. Should work reasonably well. Configuration is stored in a Mongo Database. |


 
# General workflow

1. An user register itself in the application.
2. An user can then create a group. The group will share a common set of database. The user is automatically the administrator
of the group and can add either other administrators or clients to the group. Note at the group creation, a schema is
automatically created for the group in the default server.
3. An administrator of the group can define:
   * New server in addition of the default server
   * New schema inside the server
   * New DataProviders. A DataProvider is the "thing" actually reading the data. As of know the only DataProvider is SQL, 
   so a DataProvider can be roughly defined as a SQL query. DataProviders are not visible to the clients. There are three 
   ways of creating a DataProviders:
     * By providing a SQL query 
     * By importing data from a CSV file in SQL table. Note that the SQL table can then be reused in other DataProvider
     * By providing all the information of the DataProvider
   * New DataSource. The DataSource is visible to the client. Apart from the reference of the DataProvider, the 
   DataSource defined the access rights of the client (Read/Write/Delete).
4. Members of the group can then browse the list of DataSources and access each retrieve their definition and manipulate
the data with simple JSON query. Clients do not have to handle the complexity of the underlying configuration.

# DataProvider
As said before the DataProvider is the definition of the data as retrieved from the DataBase. That makes them an 
important of the configuration. Their structure is however simple. They have:

 * a name,
 * an attribute editable,
 * a SQL query for the SQL DataProviders. But it is easy to create new providers that may rely on other type of data 
 storage,
 * a list of columns.
 
## Column definitions

### Standard Column
A standard column is a column having data. It is defined with the following attributes

 * a name,
 * a maximum size,
 * an data type:  STRING, LONG, DOUBLE, BOOLEAN, DATE, TIME, DATE_TIME, LIST_OF_STRINGS,
 * an attribute editable,
 * storage information defining the attributes more tied to the database
   * readAttributeName: the name of the column in the query
   * nullable: if the column is nullable
   * primaryKey: if the column is primary key
   * autoIncrement: if the column is autoIncrement
   * containerName: The name of the table in which to write the data
   * writeAttributeName: the name of the column in the table in which to write the data
   
Separating the definition of the read column and of the write column can be useful in some complex cases, where for 
example the query is returning a computed value (TO_LOWER(), etc.), so a non writable column but for which the data can 
still be written. 
   
### Lookup column
A lookup column holds one or multiple values (LIST_OF_STRINGS) that are used for retrieving data from another table. 
For example, the values in the column may be `"COLOR1"`, `"COLOR2"` or `"COLOR3"`. Then another table is holding the 
definitions of `"COLOR1"=>"Red"`, `"COLOR2"=>"Green"` or `"COLOR3"=>"Blue"`

When read or written from the DataSource by the clients, the final values are directly returned. In addition of the 
standard column attributes, the following are added:

 * maximumNumberOfLookups: The maximum number of lookup that the column can have
 * dataSourceId: The id of the DataSource having the codification
 * keyColumnName: The name of the column having the key (`"COLOR1"`, etc.)
 * valueColumnName: The name of the column having the value (`"Red"`, etc.)

# Documentation

The complete documentation is to be written...
 
## Configuration

### Configuring the server
An example of configuration, hopefully enough commented is given in `app/config/application.yml`

### Registering an User

endpoint: `[POST] /public/v1/registration`

example input: 
```json
{
  "firstName": "thomas",
  "lastName": "wuillemin", 
  "userName": "thomas.wuillemin@gmail.com",
  "password": "p4ssw0rd",
  "profile": "USER",
  "enabled": true,
  "participatingGroupIds": []
}
```

Notes:

 * For `userName` it is recommended to an email. If a Gmail is given, user will the be able to authenticate
 with its current Google user. Also the user name must be unique in the system.
 * The `password` is obviously not kept in clear (BCrypt hashed)
 * The values given in `profile` and `enabled` are ignored.
 * The `participatingGroupIds` must be given empty.
 
### Authenticating an User
There are two ways of authenticating a user:

 * By user password, endpoint: `[POST] /authentication/v1/internal/login`. In this case the input must be:
 ```json
{
  "userName": "thomas.wuillemin@gmail.com",
  "password": "p4ssw0rd"
}
 ```
 
 * By using the connected Google user, enpoint: `[GET] /authentication/v1/external/login`. In this case, no input is necessary.
 
For both authentication, the endpoint will return JWT token information: 
```json
{
  "authenticationToken": "eyJhbGciOiJSU[...]",
  "authenticationTokenExpiration": 1557952794,
  "refreshToken": "247effcb-9bb3-4b29-bdf2-606107dbb5fd",
  "refreshTokenExpiration": 1557963594
}
```

Notes:

 * Times in the token are given from epoch in ms.
 * It is the responsibility of the client to refresh the token using the endpoint `[POST] /authentication/v1/refresh` 
 with a query like: 
```json
{
  "refreshToken": "247effcb-9bb3-4b29-bdf2-606107dbb5fd"
}
```

### Registering a new Group

endpoint: `[POST] /api/common/v1/groups`

example input: 
```json
{
  "name": "Beautiful Group",
  "administratorIds": [],
  "userIds": []
}
```

Notes:
 * The name of the group must be unique system wide.
 * It is possible to id of existing users to `administratorIds` and `userIds`, but in any case the user creating the 
 group will defined as an administrator.
 
During the group creation, a schema is automatically created in the default database. It is possible to retrieve the 
list of schema accessible to an user with the endpoint `[GET] /api/dataserver/v1/configuration/schemas`

### Importing Data

endpoint: `[POST] /api/dataserver/v1/configuration/dataProviders/autoImportFromCSV`

```json
{
  "schemaId": "id of the schema",
  "tableName": "Cat",
  "dataBase64": "TmlycnRpLGZlbWFsZQpDcm9udXMsbWFsZQ=="
}
```

Notes:
 * Data are to be sent in base64 format.
 * The import will automatically detect the best format for each column.
 * The result of the import a fully defined DataProvider. This DataProvider can then be modified for defining the 
 primary keys, etc.
 
### Creating a DataProvider

There are other ways of creating DataProvider: either from a SQL query on existing data, or by inputting directly a 
DataProvider object.  

### Creating a data source

endpoint: `[POST] /api/dataserver/v1/configuration/dataSources`

```json
{
  "dataProviderId": "id of the data provider",
  "name": "Cat",
  "userAllowedToReadIds": [],
  "userAllowedToWriteIds": [],
  "userAllowedToDeleteIds": []
}
```

Notes:
 * It is not needed to add the administrators of the group as they are... administrators. 

## Client usage

As said before, the client is only dealing with DataSources. Nothing of the previously created object are needed, or 
even visible

### Discovering
For discovering, a client can use the following endpoints:

 * `[GET] /api/dataserver/v1/client/readableDataSources` for retrieving the list of all readable DataSources
 * `[GET] /api/dataserver/v1/client/{dataSourceId}/columns` for getting the columns of a DataSource

### Reading

endpoint: `[GET] /api/dataserver/v1/client/{dataSourceId}/data`

```json
{
  "filter": {},
  "orders": [
    {
      "columnName": "name of the column",
      "direction": "ASCENDING"
    }
  ],
  "indexFirstRecord": 100,
  "numberOfRecords": 50
}
```

### Filters
Filters allow the client to provider a predicate that is applied to the query for retrieving the data. Filters are
JSON objects. Although more verbose than a SQL filter, their syntax is consistent and easy to understand.

Example:
```json
{
  "filter": {
    "type": "And",
    "predicates": [
    {
      "type": "Or",
      "predicates": [
      {
        "type": "Equal",
        "left": {
          "type": "ColumnName",
          "name": "columnA"
        },
        "right": {
          "type": "Value",
          "value" : 25
        }
      },
      {
        "type": "In",
        "column": {
          "type": "ColumnName",
          "name": "columnB"
        },
        "values": [
        {
          "type": "Value",
          "value" : "A"
        },
        {
          "type": "Value",
          "value" : "B"
        }
        ]
      }
      ]
    },
    {
      "type": "StartsWith",
      "column": {
        "type": "ColumnName",
        "name": "columnC"
      },
      "value": {
        "type": "Value",
        "value" : "QwErTy"
      },
      "caseSensitive": false
    }
    ]
  }
}
``` 

is roughly equivalent to SQL:

```sql
WHERE (columnA = 25 AND columnB IN ('A', 'B')) OR TO_LOWER(columnC) LIKE 'qwerty%'

```

Each object in the filter (request element) is composed of a `type` attribute defining its role and of various attributes. A 
request element can be a:
 * Value: attributes are `"type":"Value"` and `"value":<Any>`
 * Column: attributes are `"type":"Value"` and `"value":<Any>`
 * Predicate: cf. list here under

The following predicates are available:

| type | First parameter | Second parameter | Third parameter |
| --- | --- | --- | --- |
| And | predicates: a list of other predicates |   |   |
| Or | predicates: a list of other predicates |   |   |
| Equal | left: the RE on the left hand side | right: the RE on the left right side |   |
| NotEqual | left: the RE on the left hand side | right: the RE on the left right side |   |
| GreaterThan | left: the RE on the left hand side | right: the RE on the left right side |   |
| GreaterThanOrEqual | left: the RE on the left hand side | right: the RE on the left right side |   |
| LowerThan | left: the RE on the left hand side | right: the RE on the left right side |   |
| LowerThanOrEqual | left: the RE on the left hand side | right: the RE on the left right side |   |
| Contains | column: a Column | value: a Value  | caseSensitive: boolean  |
| EndsWith | column: a Column | value: a Value  | caseSensitive: boolean  |
| StartsWith | column: a Column | value: a Value  | caseSensitive: boolean  |
| In | column: a Column | values: a list of Values  |  |
| Not | column: a Column | values: a list of Values  |  |


### Writing
_TBW_

### Deleting
_TBW_

# How-to

If you think something like: "Wow, that seems cool, I am not sure if I can use it. It seems complicated", don't hesitate
to send me a mail and we will think how we can do it. 

# License

Copyright 2019 Thomas Wuillemin  <thomas.wuillemin@gmail.com>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this project or its content except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
