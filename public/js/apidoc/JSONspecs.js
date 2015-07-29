


function getSpecs() {


	var jsonSpec = 


/////////////////////////////////////////////////////////
//
//		PASTE JSON FROM SWAGGER EDITOR
//			HERE :
//
////////////////////////////////////////////////////////




	{
		    "swagger": "2.0",
		    "info": {
		        "version": "v1",
		        "title": "WITH API",
		        "description": "Welcome to the WITH API documentation! \nWe are still in a development phase, so expect frequent changes. We will keep this documentation updated and this text will include a memo of the latest changes. You can read the full log here:\n```\nhttp://coming.soon.com\n```\n"
		    },
		    "paths": {
		        "/api/search": {
		            "post": {
		                "tags": [
		                    "Search"
		                ],
		                "summary": "General search in external resources and the WITH database.",
		                "description": "Body contains search parameters, response is a JSON array of records that match the search term. Boolean search supports use of AND, OR and NOT operators. Terms seperated without an operator (using a space) are treated as an AND. Use of quotes will perform exact term or phrase searches. For example, ```\"Olympian Zeus\"``` will search for the exact phrase, whereas ```Olympian Zeus``` will equate to ```Olympian AND Zeus```. For more search options use advanced search (coming soon).",
		                "parameters": [
		                    {
		                        "in": "body",
		                        "name": "body",
		                        "required": true,
		                        "description": "Search parameters.",
		                        "schema": {
		                            "type": "object",
		                            "properties": {
		                                "searchTerm": {
		                                    "type": "string"
		                                },
		                                "page": {
		                                    "type": "integer"
		                                },
		                                "pageSize": {
		                                    "type": "integer"
		                                },
		                                "source": {
		                                    "type": "string"
		                                }
		                            }
		                        }
		                    }
		                ],
		                "responses": {
		                    "200": {
		                        "description": "OK",
		                        "schema": {
		                            "type": "array",
		                            "items": {
		                                "$ref": "#/definitions/Record"
		                            }
		                        }
		                    },
		                    "403": {
		                        "description": "Bad request",
		                        "schema": {
		                            "properties": {
		                                "error": {
		                                    "type": "string"
		                                }
		                            }
		                        }
		                    }
		                }
		            }
		        },
		        "/collection/list": {
		            "get": {
		                "parameters": [
		                    {
		                        "name": "offset",
		                        "in": "query",
		                        "description": "Offset",
		                        "type": "integer"
		                    },
		                    {
		                        "name": "count",
		                        "in": "query",
		                        "description": "Count (default 10)",
		                        "type": "integer"
		                    },
		                    {
		                        "name": "access",
		                        "in": "query",
		                        "description": "Type of access (read, write, owned - default is owned)",
		                        "type": "string"
		                    },
		                    {
		                        "name": "filterByUser",
		                        "in": "query",
		                        "description": "Owned by this user (username)",
		                        "type": "string"
		                    },
		                    {
		                        "name": "filterByUserId",
		                        "in": "query",
		                        "description": "Owned by this user (user ID)",
		                        "type": "string"
		                    },
		                    {
		                        "name": "filterByUserEmail",
		                        "in": "query",
		                        "description": "Owned by this user (user email)",
		                        "type": "string"
		                    }
		                ],
		                "summary": "Get a list of collections.",
		                "description": "Using the parameter filters, you can get the collections associated with a specific user.",
		                "tags": [
		                    "Collection"
		                ],
		                "responses": {
		                    "200": {
		                        "description": "OK",
		                        "schema": {
		                            "type": "array",
		                            "items": {
		                                "$ref": "#/definitions/Collection"
		                            }
		                        }
		                    }
		                }
		            }
		        },
		        "/collection/create": {
		            "post": {
		                "tags": [
		                    "Collection"
		                ],
		                "summary": "Create a new collection.",
		                "description": "This creates a new collection and stores it in the database. You can add records to it later with  ```/collection/{collectionId}/addRecord```. Fields with asterisk are required.",
		                "parameters": [
		                    {
		                        "in": "body",
		                        "name": "body",
		                        "description": "Collection metadata",
		                        "required": true,
		                        "schema": {
		                            "type": "object",
		                            "required": [
		                                "ownerId",
		                                "title",
		                                "isPublic"
		                            ],
		                            "properties": {
		                                "ownerId": {
		                                    "type": "string"
		                                },
		                                "title": {
		                                    "type": "string"
		                                },
		                                "description": {
		                                    "type": "string"
		                                },
		                                "isPublic": {
		                                    "type": "boolean"
		                                },
		                                "rights": {
		                                    "type": "string"
		                                }
		                            }
		                        }
		                    }
		                ],
		                "responses": {
		                    "200": {
		                        "description": "OK",
		                        "schema": {
		                            "$ref": "#/definitions/Collection"
		                        }
		                    },
		                    "403": {
		                        "description": "Bad Request",
		                        "schema": {
		                            "properties": {
		                                "error": {
		                                    "type": "string"
		                                }
		                            }
		                        }
		                    },
		                    "500": {
		                        "description": "Internal Server Error",
		                        "schema": {
		                            "properties": {
		                                "error": {
		                                    "type": "string"
		                                }
		                            }
		                        }
		                    }
		                }
		            }
		        },
		        "/collection/{collectionId}/addRecord": {
		            "parameters": [
		                {
		                    "name": "collectionId",
		                    "in": "path",
		                    "description": "Id of the collection or exhibition",
		                    "type": "string"
		                }
		            ],
		            "post": {
		                "description": "Adds a record to the specified collection, creating a new record that containts the specified metadata. Note that calls to this path can also be used for exhibitions.",
		                "summary": "Add a record to a collection.",
		                "tags": [
		                    "Collection",
		                    "Exhibition"
		                ],
		                "parameters": [
		                    {
		                        "in": "body",
		                        "name": "body",
		                        "description": "Record JSON schema",
		                        "schema": {
		                            "$ref": "#/definitions/Record"
		                        }
		                    }
		                ],
		                "responses": {
		                    "200": {
		                        "description": "OK",
		                        "schema": {
		                            "$ref": "#/definitions/Record"
		                        }
		                    },
		                    "500": {
		                        "description": "Internal Server Error"
		                    }
		                }
		            }
		        },
		        "/collection/{collectionId}/removeRecord": {
		            "parameters": [
		                {
		                    "name": "collectionId",
		                    "in": "path",
		                    "description": "Id of the collection or exhibition from which to remove the record",
		                    "type": "string"
		                }
		            ],
		            "delete": {
		                "description": "Removes the specified record from a specified collection. Note that calls to this path can also be used for exhibitions.",
		                "summary": "Remove a record from a collection",
		                "tags": [
		                    "Collection",
		                    "Exhibition"
		                ],
		                "parameters": [
		                    {
		                        "name": "recordId",
		                        "in": "query",
		                        "description": "Id of record to be removed",
		                        "type": "string"
		                    }
		                ],
		                "responses": {
		                    "200": {
		                        "description": "OK"
		                    },
		                    "403": {
		                        "description": "Forbidden"
		                    },
		                    "500": {
		                        "description": "Internal Server Error"
		                    }
		                }
		            }
		        },
		        "/collection/{collectionId}/list": {
		            "parameters": [
		                {
		                    "name": "collectionId",
		                    "in": "path",
		                    "description": "Id of the collection",
		                    "type": "string"
		                }
		            ],
		            "get": {
		                "description": "Retrieves all records from the collection specified in the path and returns an array of record objects. The format parameter defines the format of this array.",
		                "summary": "Retrieve all records in a collection.",
		                "tags": [
		                    "Collection"
		                ],
		                "parameters": [
		                    {
		                        "name": "start",
		                        "in": "query",
		                        "description": "offset",
		                        "type": "integer"
		                    },
		                    {
		                        "name": "count",
		                        "in": "query",
		                        "description": "count (default 10)",
		                        "type": "integer"
		                    },
		                    {
		                        "name": "format",
		                        "in": "query",
		                        "description": "One of the following:  JSON_UNKNOWN, JSONLD_UNKNOWN, XML_UNKNOWN, JSON_EDM, JSONLD_EDM, XML_EDM, JSONLD_DPLA, JSON_NLA, XML_NLA, JSON_DNZ, XML_DNZ, JSON_YOUTUBE, “UKNOWN”, “all”. If not specified, no content is returned, only basic collection fields.",
		                        "type": "string"
		                    }
		                ],
		                "responses": {
		                    "200": {
		                        "description": "OK",
		                        "schema": {
		                            "$ref": "#/definitions/Record"
		                        }
		                    },
		                    "403": {
		                        "description": "Forbiden"
		                    },
		                    "500": {
		                        "description": "Internal Server Error"
		                    }
		                }
		            }
		        },
		        "/collection/{collectionId}": {
		            "parameters": [
		                {
		                    "name": "collectionId",
		                    "in": "path",
		                    "description": "Internal id of the collection/exhibition",
		                    "type": "string"
		                }
		            ],
		            "get": {
		                "summary": "Retrieves collection metadata.",
		                "description": "Returns the metadata of the collection specified in path. Note that calls to this path can also be used for exhibitions.",
		                "tags": [
		                    "Collection",
		                    "Exhibition"
		                ],
		                "responses": {
		                    "200": {
		                        "description": "OK",
		                        "schema": {
		                            "$ref": "#/definitions/Collection"
		                        }
		                    },
		                    "403": {
		                        "description": "Forbidden (No read-access)"
		                    },
		                    "500": {
		                        "description": "Internal Server Error(Database error)"
		                    }
		                }
		            },
		            "post": {
		                "summary": "Updates collection metadata.",
		                "description": "Use this call to change the stored metadata of a collection. Note that calls to this path can also be used for exhibitions.",
		                "tags": [
		                    "Collection",
		                    "Exhibition"
		                ],
		                "parameters": [
		                    {
		                        "in": "body",
		                        "name": "body",
		                        "description": "New collection/exhibtion metadata.",
		                        "required": false,
		                        "schema": {
		                            "type": "object",
		                            "properties": {
		                                "title": {
		                                    "type": "string"
		                                },
		                                "isPublic": {
		                                    "type": "boolean"
		                                },
		                                "description": {
		                                    "type": "string"
		                                }
		                            }
		                        }
		                    }
		                ],
		                "responses": {
		                    "200": {
		                        "description": "OK",
		                        "schema": {
		                            "$ref": "#/definitions/Collection"
		                        }
		                    },
		                    "400": {
		                        "description": "Wrong JSON fields"
		                    },
		                    "403": {
		                        "description": "Forbidden (No read-access)"
		                    },
		                    "500": {
		                        "description": "Internal Server Error (Database Error)"
		                    }
		                }
		            },
		            "delete": {
		                "summary": "Deletes the collection.",
		                "description": "Removes a collection from the database. Records that were created ?Note that calls to this path can also be used for exhibitions.",
		                "tags": [
		                    "Collection",
		                    "Exhibition"
		                ],
		                "responses": {
		                    "200": {
		                        "description": "OK",
		                        "schema": {
		                            "$ref": "#/definitions/Collection"
		                        }
		                    },
		                    "403": {
		                        "description": "No read-access"
		                    },
		                    "500": {
		                        "description": "Database error"
		                    }
		                }
		            }
		        },
		        "/record/{recordId}": {
		            "parameters": [
		                {
		                    "name": "recordId",
		                    "in": "path",
		                    "required": true,
		                    "description": "The id of the record",
		                    "type": "string"
		                }
		            ],
		            "get": {
		                "summary": "Retrieves a record.",
		                "description": "Retrieve a JSON with the metadata of the record specified in the path.",
		                "tags": [
		                    "Record"
		                ],
		                "responses": {
		                    "200": {
		                        "description": "OK",
		                        "schema": {
		                            "$ref": "#/definitions/Record"
		                        }
		                    },
		                    "500": {
		                        "description": "Internal Server Error"
		                    }
		                }
		            },
		            "post": {
		                "summary": "Updates a record.",
		                "description": "Update the metadata of an existing record, specified by its id in the path.",
		                "parameters": [
		                    {
		                        "in": "body",
		                        "name": "body",
		                        "description": "A JSON with the updated metadata",
		                        "schema": {
		                            "$ref": "#/definitions/Record"
		                        }
		                    }
		                ],
		                "tags": [
		                    "Record"
		                ],
		                "responses": {
		                    "200": {
		                        "description": "OK",
		                        "schema": {
		                            "$ref": "#/definitions/Record"
		                        }
		                    },
		                    "500": {
		                        "description": "Internal Server Error"
		                    }
		                }
		            },
		            "delete": {
		                "summary": "Removes a record.",
		                "description": "Remove the record specified in the path from the database.",
		                "tags": [
		                    "Record"
		                ],
		                "responses": {
		                    "200": {
		                        "description": "OK",
		                        "schema": {
		                            "$ref": "#/definitions/Record"
		                        }
		                    },
		                    "500": {
		                        "description": "Internal Server Error"
		                    }
		                }
		            }
		        },
		        "/rights/{collectionId}/{right}": {
		            "parameters": [
		                {
		                    "name": "collectionId",
		                    "in": "path",
		                    "required": true,
		                    "description": "Internal Id of the collection whose rights you wish to change",
		                    "type": "string"
		                },
		                {
		                    "name": "right",
		                    "in": "path",
		                    "required": true,
		                    "description": "\"none\" (withdraws previously given rights), \"read\", \"write\", \"own\"",
		                    "type": "string"
		                }
		            ],
		            "post": {
		                "summary": "Set rights",
		                "description": "Changes access rights (read, write, own) of a specified user for a specifed collection. Only the owner of a collection can use this call. One of username, email or userId need to be provided.",
		                "tags": [
		                    "Rights",
		                    "Collection"
		                ],
		                "parameters": [
		                    {
		                        "name": "username",
		                        "in": "query",
		                        "description": "username of user to give rights to (or take away from)",
		                        "type": "string"
		                    },
		                    {
		                        "name": "email",
		                        "in": "query",
		                        "description": "another way of specifying the user",
		                        "type": "string"
		                    },
		                    {
		                        "name": "userId",
		                        "in": "query",
		                        "description": "another way of specifying the user",
		                        "type": "string"
		                    }
		                ],
		                "responses": {
		                    "200": {
		                        "description": "OK"
		                    },
		                    "403": {
		                        "description": "Bad Request"
		                    },
		                    "500": {
		                        "description": "Interal Server Error"
		                    }
		                }
		            }
		        },
		        "/exhibition/create": {
		            "post": {
		                "tags": [
		                    "Exhibition"
		                ],
		                "summary": "Create a new exhibition.",
		                "description": "Creates a new exhibition with a unique dummy title that can be changed later. You can make a POST call to ```/collection/{collectionId}``` to edit an exhibition.",
		                "parameters": [
		                    {
		                        "in": "body",
		                        "name": "body",
		                        "description": "Contains metadata about the exhibition",
		                        "schema": {
		                            "$ref": "#/definitions/Exhibition"
		                        }
		                    }
		                ],
		                "responses": {
		                    "200": {
		                        "description": "OK",
		                        "schema": {
		                            "$ref": "#/definitions/Exhibition"
		                        }
		                    },
		                    "400": {
		                        "description": "Invalid JSON"
		                    },
		                    "500": {
		                        "description": "Database Error"
		                    }
		                }
		            }
		        },
		        "/exhibition/list": {
		            "get": {
		                "tags": [
		                    "Exhibition"
		                ],
		                "summary": "Get all user exhibitons.",
		                "description": "Returns an array of exhibition JSON objects, of all exhibitions owned by the currently logged in user. See ```/user/login```.",
		                "parameters": [
		                    {
		                        "name": "offset",
		                        "description": "Offset (default is 0)",
		                        "in": "query",
		                        "type": "integer"
		                    },
		                    {
		                        "name": "count",
		                        "description": "Number of results (default is 10)",
		                        "in": "query",
		                        "type": "integer"
		                    }
		                ],
		                "responses": {
		                    "200": {
		                        "description": "OK",
		                        "schema": {
		                            "type": "array",
		                            "items": {
		                                "$ref": "#/definitions/Exhibition"
		                            }
		                        }
		                    },
		                    "403": {
		                        "description": "Forbidden"
		                    }
		                }
		            }
		        },
		        "/user/register": {
		            "post": {
		                "tags": [
		                    "User"
		                ],
		                "description": "Creates a new user and stores at the database.",
		                "summary": "Create new user.",
		                "produces": [
		                    "application/json",
		                    "application/xml"
		                ],
		                "parameters": [
		                    {
		                        "in": "body",
		                        "name": "body",
		                        "description": "Contains JSON of the user to create",
		                        "required": true,
		                        "schema": {
		                            "type": "object",
		                            "properties": {
		                                "firstName": {
		                                    "type": "string"
		                                },
		                                "lastName": {
		                                    "type": "string"
		                                },
		                                "username": {
		                                    "type": "string"
		                                },
		                                "email": {
		                                    "type": "string"
		                                },
		                                "password": {
		                                    "type": "string"
		                                },
		                                "gender": {
		                                    "type": "string"
		                                },
		                                "facebookId": {
		                                    "type": "string"
		                                },
		                                "googleID": {
		                                    "type": "string"
		                                },
		                                "about": {
		                                    "type": "string"
		                                },
		                                "location": {
		                                    "type": "string"
		                                }
		                            }
		                        }
		                    }
		                ],
		                "responses": {
		                    "200": {
		                        "description": "OK",
		                        "schema": {
		                            "$ref": "#/definitions/User"
		                        }
		                    },
		                    "400": {
		                        "description": "Bad Request"
		                    }
		                }
		            }
		        },
		        "/user/login": {
		            "post": {
		                "tags": [
		                    "User"
		                ],
		                "summary": "User login.",
		                "description": "Log an user in (create a browser cookie). Some API calls do not take the user as a parameter and you need to be logged in first.",
		                "parameters": [
		                    {
		                        "in": "body",
		                        "name": "body",
		                        "description": "Email or username and password",
		                        "required": true,
		                        "schema": {
		                            "type": "object",
		                            "properties": {
		                                "email": {
		                                    "type": "string"
		                                },
		                                "password": {
		                                    "type": "string"
		                                }
		                            }
		                        }
		                    }
		                ],
		                "responses": {
		                    "200": {
		                        "description": "OK status, login cookie, user metadata JSON including userID.",
		                        "schema": {
		                            "$ref": "#/definitions/User"
		                        }
		                    },
		                    "400": {
		                        "description": "Error status, problem description JSON."
		                    }
		                }
		            }
		        },
		        "/user/logout": {
		            "get": {
		                "description": "Browser cookie is removed, user i logged out (all session information is kept in cookie, nothing is stored on server).",
		                "summary": "User logout.",
		                "tags": [
		                    "User"
		                ],
		                "responses": {
		                    "200": {
		                        "description": "OK"
		                    }
		                }
		            }
		        },
		        "/user/emailAvailable": {
		            "get": {
		                "tags": [
		                    "User"
		                ],
		                "summary": "Check email availability.",
		                "description": "Used when registering a new user, checks if there has been another user with the same email already stored in the database.",
		                "parameters": [
		                    {
		                        "name": "email",
		                        "in": "query",
		                        "description": "Proposed email address",
		                        "required": true,
		                        "type": "string"
		                    }
		                ],
		                "responses": {
		                    "200": {
		                        "description": "OK"
		                    },
		                    "400": {
		                        "description": "Not available"
		                    }
		                }
		            }
		        },
		        "/user/{userId}": {
		            "parameters": [
		                {
		                    "name": "userId",
		                    "in": "path",
		                    "description": "Internal ID of a user",
		                    "required": true,
		                    "type": "string"
		                }
		            ],
		            "get": {
		                "summary": "Get user details.",
		                "description": "Returns the complete entry of a user specified by the id provided in the path.",
		                "tags": [
		                    "User"
		                ],
		                "responses": {
		                    "200": {
		                        "description": "OK",
		                        "schema": {
		                            "$ref": "#/definitions/User"
		                        }
		                    }
		                }
		            },
		            "put": {
		                "tags": [
		                    "User"
		                ],
		                "summary": "Update an user entry.",
		                "description": "Updates the stored info of the user specified by the id provided in the path.",
		                "parameters": [
		                    {
		                        "in": "body",
		                        "name": "body",
		                        "description": "New user entry",
		                        "required": true,
		                        "schema": {
		                            "$ref": "#/definitions/User"
		                        }
		                    }
		                ],
		                "responses": {
		                    "200": {
		                        "description": "OK",
		                        "schema": {
		                            "$ref": "#/definitions/User"
		                        }
		                    }
		                }
		            },
		            "delete": {
		                "tags": [
		                    "User"
		                ],
		                "summary": "Deletes the user.",
		                "description": "Removes a user from the database.",
		                "responses": {
		                    "200": {
		                        "description": "OK"
		                    }
		                }
		            }
		        },
		        "/user/resetPassword/{emailOrUserName}": {
		            "parameters": [
		                {
		                    "name": "emailOrUserName",
		                    "in": "path",
		                    "description": "Username or email",
		                    "type": "string"
		                }
		            ],
		            "get": {
		                "tags": [
		                    "User"
		                ],
		                "summary": "Reset password email.",
		                "description": "Sends an email to the user provided in the path. The email contains a link to a webpage where the user can provide a new password.",
		                "responses": {
		                    "200": {
		                        "description": "OK"
		                    }
		                }
		            }
		        },
		        "/user/apikey/create": {
		            "post": {
		                "tags": [
		                    "User"
		                ],
		                "parameters": [
		                    {
		                        "name": "email",
		                        "in": "query",
		                        "description": "An email",
		                        "required": false,
		                        "type": "string"
		                    }
		                ],
		                "summary": "Get an API key.",
		                "description": "If a user is logged in, automatically sends an API key to the stored email address. Alternatively, sends an email to the email address provided as a parameter. If both are true, only the provided email address will be used.",
		                "responses": {
		                    "200": {
		                        "description": "OK"
		                    },
		                    "403": {
		                        "description": "Bad Request"
		                    },
		                    "500": {
		                        "description": "Internal Server Error"
		                    }
		                }
		            }
		        }
		    },
		    "definitions": {
		        "User": {
		            "type": "object",
		            "required": [
		                "firstName",
		                "lastName",
		                "username",
		                "email",
		                "password",
		                "about",
		                "location"
		            ],
		            "properties": {
		                "firstName": {
		                    "type": "string"
		                },
		                "lastName": {
		                    "type": "string"
		                },
		                "username": {
		                    "type": "string"
		                },
		                "email": {
		                    "type": "string"
		                },
		                "password": {
		                    "type": "string"
		                },
		                "gender": {
		                    "type": "string"
		                },
		                "facebookId": {
		                    "type": "string"
		                },
		                "googleID": {
		                    "type": "string"
		                },
		                "about": {
		                    "type": "string"
		                },
		                "location": {
		                    "type": "string"
		                },
		                "userId": {
		                    "type": "string"
		                }
		            }
		        },
		        "Collection": {
		            "type": "object",
		            "properties": {
		                "id": {
		                    "type": "object",
		                    "properties": {
		                        "value": {
		                            "type": "string"
		                        }
		                    }
		                },
		                "ownerId": {
		                    "type": "object",
		                    "properties": {
		                        "value": {
		                            "type": "string"
		                        }
		                    }
		                },
		                "className": {
		                    "type": "string"
		                },
		                "title": {
		                    "type": "string"
		                },
		                "description": {
		                    "type": "string"
		                },
		                "itemCount": {
		                    "type": "integer"
		                },
		                "isPublic": {
		                    "type": "boolean"
		                },
		                "rights": {
		                    "type": "object",
		                    "properties": {
		                        "value": {
		                            "type": "string"
		                        }
		                    }
		                }
		            }
		        },
		        "Exhibition": {
		            "type": "object",
		            "properties": {
		                "id": {
		                    "type": "object",
		                    "properties": {
		                        "value": {
		                            "type": "string"
		                        }
		                    }
		                },
		                "ownerId": {
		                    "type": "object",
		                    "properties": {
		                        "value": {
		                            "type": "string"
		                        }
		                    }
		                },
		                "className": {
		                    "type": "string"
		                },
		                "title": {
		                    "type": "string"
		                },
		                "description": {
		                    "type": "string"
		                },
		                "itemCount": {
		                    "type": "integer"
		                },
		                "isPublic": {
		                    "type": "boolean"
		                },
		                "rights": {
		                    "type": "object",
		                    "properties": {
		                        "value": {
		                            "type": "string"
		                        }
		                    }
		                },
		                "isExhibition": {
		                    "type": "boolean"
		                },
		                "exhibition": {
		                    "type": "object",
		                    "properties": {
		                        "intro": {
		                            "type": "string"
		                        }
		                    }
		                }
		            }
		        },
		        "Record": {
		            "type": "object",
		            "required": [
		                "title"
		            ],
		            "properties": {
		                "dbId": {
		                    "type": "string"
		                },
		                "externalId": {
		                    "type": "string"
		                },
		                "isPublic": {
		                    "type": "boolean"
		                },
		                "source": {
		                    "type": "string"
		                },
		                "thumbnailUrl": {
		                    "type": "string"
		                },
		                "title": {
		                    "type": "string"
		                },
		                "creator": {
		                    "type": "string"
		                },
		                "description": {
		                    "type": "string"
		                },
		                "provider": {
		                    "type": "string"
		                },
		                "sourceId": {
		                    "type": "string"
		                },
		                "sourceUrl": {
		                    "type": "string"
		                },
		                "exhibition": {
		                    "type": "object",
		                    "properties": {
		                        "anotation": {
		                            "type": "string"
		                        },
		                        "audioUrl": {
		                            "type": "string"
		                        },
		                        "videoUrl": {
		                            "type": "string"
		                        }
		                    }
		                }
		            }
		        }
		    }
		}







///////////////////////////////////



///////////////////////////////////
;

return jsonSpec;

}















function apiKeyClick(){
	
	$.ajax({
		type    : "get",
		url     : "/user/apikey/create",
		success : function(data) {
			// Show message that an email was sent
			$("#myModal").find("h4").html("API key requested!");
			$("#myModal").find("#popupText").html("<p>An email was successfuly sent. " +
					"Follow the instructions to create a new password.</p>");
			$("#myModal").modal('show');
		},
		error   : function(request, status, error) {
			//var err = JSON.parse(request.responseText, function(k,v){
			//	alert(k);
			//	alert(v);
			//			});

			var err = JSON.parse(request.responseText);
			$("#myModal").find("h4").html("Email not sent");
			$("#myModal").find("#popupText").html("<p>" + err.error.error + "</p>");
			$("#myModal").find("h4").html("Email not sent");
			$("#myModal").modal('show');

		}
	});

    return false;

}


