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

Dependencies are :
 
 * A PostgreSQL server for storing the structured data
 * A MongoDB server for storing the software configuration (may change with the time) 
 
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
_TBW_

### Registering an Organization
_TBW_

### Importing Data
_TBW_

### Creating a data structure
_TBW_

## Usage
_TBW_

### Reading
_TBW_

### Writing
_TBW_

### Deleting
_TBW_

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
