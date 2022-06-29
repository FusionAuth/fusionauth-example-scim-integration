# Example SCIM Integration

This is an example SCIM integration.

You'll need an enterprise version of FusionAuth, maven and a modern version of java. Tested with java 17.

To run it:

* set up SCIM as documented in the FusionAuth documentation: https://fusionauth.io/docs/v1/tech/core-concepts/scim
* Edit the file and modify the constants at the top.
* `mvn compile` to compile it.
* `mvn exec:java -Dexec.mainClass="io.fusionauth.example.scim.ScimExample" -Dexec.args="client_secret get"`

The first argument is the client secret from the SCIM client entity.

The second is the operation. Supported operations are:

* get: retrieves a user
* create: creates a user
* list: lists users
