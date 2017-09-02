Akka Actor Model for PII Encryption: 

This project contains a model for parallel encryption of Database columns using Akka’s Actor Concurrency Library. This file will detail the high level functionality of the project, a quick introduction to akka actors, an overview of how to run the project, a brief summary of each java file in the project, and future steps to improve the project. 

————————————————————————————————————————————————————————————————————————————————-

High Level Summary of Project Functionality: 

Given user inputs of a table name and the names of columns to encrypt, the program queries the database for the relevant data and stores the entirety in memory. The data is batched out by a Batch Actor to various Select Actors (the quantity of which is user specified) that encrypt their respective batches. Upon encrypting the batch, the select actor sends the encrypted batch to an update actor, which makes the update call to the database. For every batch that a select actor receives, it creates a new update actor to perform the update call (a select actor has a boundless number of update actors under its jurisdiction). Once an update actor finishes its update, it terminates itself. 


Encryption is currently performed using an inexpensive 64 bit encryption algorithm for strings. Integer encryption uses a simple XOR key encryptor. 

The Actor system uses a connection pool to perform its functionality. A pool is created upon creation of the actor system, and every update Actor draws a new connection from the pool to perform its update, terminating the connection after the completion of the update. 

————————————————————————————————————————————————————————————————————————————————-

Quick Introduction to Akka: 

Actors are designed as abstractions that receive messages, and upon receiving those messages, either relay those messages to other actors or perform certain actors. In java, Actors implement the Abstract Class AbstractActor. Abstract Actor requires implementation of the method createReceive(), which underlines how an actor acts upon receiving a message. createReceive uses the method match, whose first argument is an object type, and whose second argument is a lambda function dictating how an actor should act upon receiving a message of that particular object type. 

Example: 

createReceive() {
	return receiveBuilder() 
		.match(Batch.class, 
			b -> 
			{
				Batch e = encryptBatch(b);
				updateActor.tell(b);
			});
	
}

This actor, upon receiving a batch, encrypts it and sends it to the update Actor. Notice the “tell” function. To relay a message to another actor, the recipient actor must call the tell function, with an argument of the message to be relayed. The above example more or less specifies the functionality of the Select Actor. 

In addition to dictating actor receive behavior, one must configure the actor hierarchy. Traditionally, actors receive messages from parents and relay messages to children. A child is usually represented as an instance variable within a parent actor. This child can be initialized upon receiving a message or upon creation of the parent (see SelectActor and BatchActor for respective examples). 

Instead of representing one child Actor as an instance variable, one can also hold multiple children in routing systems. A router not only encompasses multiple children, but also dictates how messages are relayed to those children. Multiple routing strategies, such as RoundRobin and SmallestMailbox, are available to utilize. 

Here is the hierarchy used in this project: 

Batch Actor (Always 1) 

	 - Select Actor (Fixed number specified by User) 

		 - Update Actor (new UpdateActor created for every batch received 				by Select Actor, no limit)

A Batch Actor receives an Encrypt Request and outputs a batch to each select actor; a select actor receives a batch and outputs an encrypted batch to a new update actor; an update actor makes the update call to the database. 

————————————————————————————————————————————————————————————————————————————————-

Overview of Running the Project: 

The Supervisor class manages the running of the project. First, run Supervisor.setup, specifying the intended number of select actors to create, the port used to connect to the database, and database name. 

Then, add a query to the queue using Supervisor.addRequest, specifying whether to decrypt or encrypt, tableName, primary key column, and all names of columns to encrypt. Multiple queries can be added to Supervisor before actor Model is run. 

Then, start the actor system by calling Supervisor.run.

Finally, close connections using Supervisor.closeConnections. 

One does not need to manually set up connections or connection pools, this will automatically be done within Supervisor.setup. 

Please only run one test at once, otherwise data be double encrypted or double decrypted. 

————————————————————————————————————————————————————————————————————————————————-

Summary of Each .java file in the project 

Database Package: 

DatabaseConnectionPool.java - Represents a connection pool implemented by the c3p0 library

DatabaseConnection.java - Represents a singular connection to the Database

SQLUtilities: Contains Object oriented system encapsulating data initially retrieved from the database. RowsToChange represents all the rows of data that need to be encrypted. Also contains tools for converting database cursor into in memory list. 

ActorModel Package: 

Supervisor.java - contains setup, runner, and cleanup actions for the actor model; Makes the select call that loads all relevant data into DB Cursors. 

BatchUtilities.java - Manages creation of batches from Encryption requests (input of Batch actor)

EncryptUtilities.java - Contains tools to encrypt and decrypt data; The utilities automatically detect object type at runtime and encrypt accordingly. 

Actor Package: 

BatchActor.java - Receives an encryption request and outputs a batch to each select actor

SelectActor.java - Receives a batch and outputs an encrypted Batch

UpdateActor.java - Receives an encrypted batch and makes an update call to the database

————————————————————————————————————————————————————————————————————————————————--

Future Steps to Improve the Project: 

Simple: 

SelectActor is a misnomer; the actor simply encrypts incoming batches and should have a name relating to encryption. The Select calls are made before the actor system is created. 

If a particular process fails for an actor, just have the actor “tell” itself the message again, so that the process can be repeated. 

Implement a Strategy in the highest level actor (BathActor) to restart any actors that may die during encryption. Akka docs will be helpful here - look for terms such as OneForOneStrategy.

Move the update Database logic from UpdateActor.java to a new UpdateUtilities.java. Then, there will be a respective utilities page for each actor type. 

Change the encryptFlag from a boolean to an enum. 

Use EncryptionUtil from IOP to encrypt Data. Currently this project does not use any IOP dependencies. 

Complicated: 

This actor model encrypts database entities table by table. However, Payroll Production seems to favor a company by company style of encryption, since tables often contain foreign keys that connect to other tables on the basis of a company. A surface level fix to the actor model that could facilitate a conversion to the company by company encryption scheme would include changes to the Select Call made in Supervisor. Just change the logic of the select call to take in a particular company and join all the particular tables that are affected by that particular company. Once the data is joined, specify all the columns you want to encrypt create an encryption request and pass the request to the batch actor. The rest of the actor model flow should be unchanged. 
 
Implement a paging system to avoid storing the entire cursor in memory. This change would be made in SQLUtilities.java. Further considerations include how much to page at one time, when paging should occur (should a new page be created after or during when a request completes?) and subsequent changes to batch partitioning (BatchUtilities.java)


————————————————————————————————————————————————————————————————————————————————

Clearing up Confusion: 

An Encryption Request (represented by the class EncryptRequest) contains the data that needs to be encrypted and is the input for a BatchActor. The request represents the encryption of particular columns within a singular table.

A Batch Actor can be loaded with multiple requests (analogous to encrypting multiple tables). Simply add more than one query to Supervisor before calling Supervisor.run(). These requests are run concurrently using the actor model. 

An Update actor pulls a connection from the pool, performs its function and then terminates itself and its connection. Therefore, having a boundless amount of update actors does not necessarily mean a boundless amount of connections since each connection is closed after the update actor performs its process. 

SelectActor does not make the Select call to extract relevant information from the database. Supervisor does. 

Currently, the project is run through ActorModelEncryptTest, so any new run configurations can be implemented there. 

The term encryptFlag as seen in the code refers to whether the user wants to encrypt or decrypt data. True refers to encryption and false refers to decryption. 


————————————————————————————————————————————————————————————————————————————————-

Contact; 

Feel free to email rahulb@berkeley.edu for any further questions concerning the Actor Model for PII Encryption.
