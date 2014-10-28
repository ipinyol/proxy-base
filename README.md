PROXY-BASE
==========

A content-based http proxy that accepts websockets. 

WARNING: The current version is still a pre-release. If you want to try it follow the instructions

About the project
=================

Proxy-base is a reverse http proxy that can be used to redirect http traffic from a single port to multiple hidden ports, by defining a set of well-defined rules based on the content of http headers. 

Two main features differentiate Proxy-base from other reverse proxies:

1.- A rule engine allows you to define not only static rules but also dynamic rules that for instance, can capture the forward port from some parameter of the http request.


2.- It supports websockets as defined in RFC 6455. It is well known that web sockets represent a challenge for proxies since the protocol changes the philosofy of http requests. Proxy-base detects the http handshake and keeps the channels open to transfer frames from the origin to the destiny and viceversa.  

Instructions 
============

Proxy-base is thought to work as a reverse proxy for normal volume of traffic. It does not handle the well known c10K problem. We are working to enable the use of Proxy-base for high traffic servers.

For instance, lets assume that your http servers serves two applications, one listening at port 8080 like the default port of JBoss, and an instance of IPython Notebook where the Tornado server listens at port 8888. You can open both ports and allow public access, however most firewalls and public wifi allow only acces to port 80. 

To solve it you just can keep ports 8080 and 8888 private and only open port 80 with Proxy-base listening to it and redirecting traffic to 8080 and 8888 accordingly. 

To try Proxy-base follow the next steps:

1.- Download the java code and build it 


2.- Edit the configuration file. After built, access the file 
    
    org/mars/proxybase/rules.config
    
and read the comments on how to write the rules


3.- Execute Proxy-base with the following command:

    java org.mars.proxybase.ProxyBase {PORT}

The system will listen to the specified port. If no port is specified, the system will use the information contained in the file

    org/mars/proxybase/config.properties

    dhost: the default destiny host if no host is informed in the rules
    dport_in: The default listening port of the system if no port is informed when executed
    dport_out: The default destiny port if no port is specified in an applied rule

