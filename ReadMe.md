# Amdatu Remote Services

Amdatu Remote Services provides implementations of the OSGi Remote Services 1.0 (OSGi Enterprise R5) and Remote Service Admin 1.1 (under development) [specifications](http://www.osgi.org/Specifications/HomePage). Amdatu Remote Services allows you to transparently remote your OSGi services using plugable discovery and transport protocols.

## Usage

To use Amdatu Remote Services, you should deploy `org.amdatu.remote.http`, `org.amdatu.remote.topology.promiscuous.jar` and one or more of the `org.amdatu.remote.discovery.*` bundles. In addition, until the RSA v1.1 APIs are formally released, the `org.osgi.service.remoteserviceadmin` bundle should be deployed as well. All services that specify the `service.exported.interfaces` service property are exported as remote services.
Discovery components are responsible for publishing exportable- as well as discovering importable service endpoints across frameworks. 
TopologyManagers are responsible for controlling the imports and exports of services using registered RemoteServiceAdmin services to do so.
RemoteServiceAdmin components are passive components that register as RemoteServiceAdmin services and are actually capable of importing and exporting certain services as defined by configuration type and constraint by intents.

### Dependencies

Amdatu Remote Services depends on the following bundles:

1. Apache Felix DependencyManager v3.1.0;
2. (Optionally) an OSGi ConfigurationAdmin service implementation, for example Apache Felix ConfigAdmin v1.8.0;
3. (Optionally) an OSGi LogService implementation, for example Apache Felix LogService v1.0.1.

### Configuration

The following subsections describe the configuration of the separate Amdatu Remote Service components.

##### Discovery Components

Configuration for Discovery components can be provided through ConfigurationAdmin or by using OSGi environment properties by creating a configuration using the Discovery component's matching PID; `org.amdatu.remote.discovery.*`, org.amdatu.discovery.etcd for example. All component types and their PIDs can be found on the [Wiki](https://amdatu.atlassian.net/wiki/display/AMDATUDEV/Amdatu+Remote). 

To provide endpoint descriptions, a Local Discovery Extender can be deployed. This Discovery implementation provides the osgi.extender capability that allows bundles to provide endpoint descriptions through generated XML files. Local Discovery Extender itself doesn't provide or require any configuration.
The Configured Discovery component has an additional `endpoints` property to directly describe service endpoints.

##### TopologyManager Components

Amdatu Remote Services currently provides a promiscuous topology manager which exports and imports every service matching to the configured imports and exports filters.
Configuration for TopologyManager components can be provided through ConfigurationAdmin or by using OSGi environment properties by creating a configuration using the following PID: `org.amdatu.remote.topology.promiscuous`. 
Configuring the promiscuous TopologyManager is done by providing import and export filters on which services to import and export. A more detailed description on how to use import and export filters can be found on the [Wiki](https://amdatu.atlassian.net/wiki/display/AMDATUDEV/Amdatu+Remote).

##### RemoteServiceAdmin Compontents

Amdatu Remote Services currently provides one RemoteServiceAdmin component which that implements the `org.amdatu.remote.admin.http` configuration type using HTTP transport. The RemoteServiceAdmin configuration can be provided through ConfigurationAdmin or by using OSGi environment properties by creating a configuration using the PID; `org.amdatu.remote.admin.http`. RemoteServiceAdmin can be configurated by providing `host`, `port`, `path`, `connecttimeout` and `readtimeout`.

The `host` property is used to configure a host name or IP Address to use for service endpoints. If not specified the service will fallback to the "org.apache.felix.http.host" environment property and default to "localhost".

The `port` property is used to configure the port number to use for service endpoints. If not specified the service will fallback to the "org.osgi.service.http.port" environment property and default to "8080".

The `path` property is used to configure the servlet path to use for service endpoints. If not specified the service will default to "org.amdatu.remote.admin.http".

The `connecttimeout` property is used to configure the timeout in ms to use when connecting to a remote service endpoint. If not specified the service will default to 5000ms.

The `readtimeout` property is used to configure the timeout in ms to use when invoking a remote service endpoint. If not specified the service will default to 60000ms.

### Example

Deployment of Amdatu Remote Services follows the selection of the implementing bundles of the desired components. By design (and specification) multiple components, and thus bundles, of each role may be deployed. However a minimal deployment involves at least one of each per framework.

Remoting services is done by exporting services, this is done by adapting the service Activator to specify which service interfaces need to be exported, and setting the correct callbacks to act upon addition and removal of service availability. 
The following example shows a simple chatclient which doesn't contain any specifics related to Remote Services:

	public interface MessageReceiver {
		void receive(String sender, String message);
	}
	
	public interface MessageSender {
		void send(String message);
	}
	
	public class ChatClient implements MessageSender, MessageReceiver {
		...
	}

And an adapted Activator which exports the client's receiver interface making it possible to call the receiver from a remote framework:

	@Override
	public void init(BundleContext context, DependencyManager manager) throws Exception {
		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put(RemoteConstants.SERVICE_EXPORTED_INTERFACES, MessageReceiver.class.getName());
		manager.add(createComponent()
		.setInterface(new String[] { MessageSender.class.getName(), MessageReceiver.class.getName() }, props)
		.setImplementation(ChatClient.class)
		.add(createServiceDependency()
			.setService(MessageReceiver.class)
			.setRequired(false)
			.setCallbacks("addReceiver", "removeReceiver")
		));
	}

After configuring and deploying both frameworks, the TopologyManagers should discover the available Discoveries and their available EndPoints. The TopologyManagers then tell a RemoteServiceAdmin service instance to import the exported service(s). The imported services are then registered in a similar way as in a non-remote OSGi environment. When injecting and calling the imported service, the RemoteServiceAdmin service acts as a proxy to relay calls and responses over the network.


## Links

* [Amdatu Website](http://www.amdatu.org/components/remote.html);
* [Source Code](https://bitbucket.org/amdatu/amdatu-remoteservices);
* [Issue Tracking](https://amdatu.atlassian.net/browse/AMDATURS);
* [Development Wiki](https://amdatu.atlassian.net/wiki/display/AMDATUDEV/Amdatu+Remote)
* [Continuous Build](https://amdatu.atlassian.net/builds/browse/AMDATURS).

## License

The Amdatu Remote Services project is licensed under [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).

