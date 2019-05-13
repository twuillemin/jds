# JSON Data Server

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

# Documentation

The documentation is to be written

## Dependencies

Dependencies are :
 
 * A PostgreSQL server for storing the structured data
 * A MongoDB server for storing the software configuration (may change with the time) 

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
