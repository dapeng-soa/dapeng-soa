package com.github.dapeng.service.invoke;

import com.github.dapeng.core.SoaSystemEnvProperties;
import com.github.dapeng.core.version.Version;
import com.github.dapeng.registry.ConfigKey;
import com.github.dapeng.registry.ServiceInfo;
import com.github.dapeng.registry.ServiceInfos;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by tangliu on 2016/2/29.
 */
public class ZookeeperWatcher {


	private ApiServices apiServices;
	private static final Logger LOGGER = LoggerFactory.getLogger(com.github.dapeng.registry.zookeeper.ZookeeperWatcher.class);

	private final boolean isClient;

	private final static Map<String, List<ServiceInfo>> caches = new ConcurrentHashMap<>();
	private final static Map<String, Map<ConfigKey, Object>> config = new ConcurrentHashMap<>();

	private ZooKeeper zk;
	private CountDownLatch connectDownLatch;

	public ZookeeperWatcher(boolean isClient,ApiServices apiServices) {
		this.isClient = isClient;
		this.apiServices = apiServices;
	}

	public void init() {
		connect();

		if (isClient) {
			getServersList();
		}

		getConfig("/soa/config");

		try {
			connectDownLatch.await(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			LOGGER.error(e.getMessage(), e);
		}
	}


	private void tryCreateNode(String path) {

		String[] paths = path.split("/");

		String createPath = "/";
		for (int i = 1; i < paths.length; i++) {
			createPath += paths[i];
			addPersistServerNode(createPath, path);
			createPath += "/";
		}
	}

	/**
	 * 添加持久化的节点
	 *
	 * @param path
	 * @param data
	 */
	private void addPersistServerNode(String path, String data) {
		Stat stat = exists(path);

		if (stat == null)
			zk.create(path, data.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT, nodeCreatedCallBack, data);
	}

	/**
	 * 判断节点是否存在
	 *
	 * @param path
	 * @return
	 */
	private Stat exists(String path) {
		Stat stat = null;
		try {
			stat = zk.exists(path, false);
		} catch (KeeperException e) {
		} catch (InterruptedException e) {
		}
		return stat;
	}

	/**
	 * 异步添加serverName节点的回调处理
	 */
	private AsyncCallback.StringCallback nodeCreatedCallBack = (rc, path, ctx, name) -> {
		switch (KeeperException.Code.get(rc)) {
			case CONNECTIONLOSS:
				LOGGER.info("创建节点:{},连接断开，重新创建", path);
				tryCreateNode((String) ctx); //每次创建都会从根节点开始尝试创建，避免根节点未创建而造成创建失败
//                addPersistServerNode(path, (String) ctx);
				break;
			case OK:
				LOGGER.info("创建节点:{},成功", path);
				break;
			case NODEEXISTS:
				LOGGER.info("创建节点:{},已存在", path);
				break;
			default:
				LOGGER.info("创建节点:{},失败", path);
		}
	};


	public void destroy() {
		if (zk != null) {
			try {
				zk.close();
				zk = null;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		caches.clear();
		config.clear();

		LOGGER.info("关闭连接，清空service info caches");
	}

	public ServiceInfos getServiceInfo(String serviceName, String versionName, boolean compatible) {

		List<ServiceInfo> serverList = caches.get(serviceName);

		List<ServiceInfo> usableList = new ArrayList<>();

		if (serverList != null && serverList.size() > 0) {
			if (!compatible) {
				usableList.addAll(serverList.stream().filter(server -> server.getVersionName().equals(versionName)).collect(Collectors.toList()));
			} else {
				usableList.addAll(serverList.stream().filter(server -> Version.toVersion(server.getVersionName()).compatibleTo(Version.toVersion(versionName))).collect(Collectors.toList()));
			}
		}
		ServiceInfos serviceInfos = new ServiceInfos(true, usableList);

		return serviceInfos;
	}

	//----------------------servicesList相关-----------------------------------

	/**
	 * 获取zookeeper中的services节点的子节点，并设置监听器
	 *
	 * @return
	 */
	public void getServersList() {

		tryCreateNode("/soa/runtime/services");

		zk.getChildren("/soa/runtime/services", watchedEvent -> {
			//Children发生变化，则重新获取最新的services列表
			if (watchedEvent.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
				LOGGER.info("{}子节点发生变化，重新获取子节点...", watchedEvent.getPath());

				getServersList();
			}
		}, (rc, path, ctx, children) -> {
			switch (KeeperException.Code.get(rc)) {
				case CONNECTIONLOSS:
					getServersList();

					break;
				case OK:
					LOGGER.info("获取services列表成功");

					resetServiceCaches(path, children);
					break;
				default:
					LOGGER.error("get services list fail");
			}
		}, null);
	}

	//----------------------servicesList相关-----------------------------------


	//----------------------serviceInfo相关-----------------------------------

	/**
	 * 对每一个serviceName,要获取serviceName下的子节点
	 *
	 * @param path
	 * @param serviceList
	 */
	private void resetServiceCaches(String path, List<String> serviceList) {
		for (String serviceName : serviceList) {
			getServiceInfoByPath(path + "/" + serviceName, serviceName);
		}
	}

	/**
	 * 根据serviceName节点的路径，获取下面的子节点，并监听子节点变化
	 *
	 * @param servicePath
	 */
	private void getServiceInfoByPath(String servicePath, String serviceName) {
		zk.getChildren(servicePath, watchedEvent -> {
			if (watchedEvent.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
				LOGGER.info("{}子节点发生变化，重新获取信息", watchedEvent.getPath());

				String[] paths = watchedEvent.getPath().split("/");
				getServiceInfoByPath(watchedEvent.getPath(), paths[paths.length - 1]);
			}
		}, (rc, path, ctx, children) -> {
			switch (KeeperException.Code.get(rc)) {
				case CONNECTIONLOSS:
					getServiceInfoByPath(path, (String) ctx);
					break;
				case OK:
					LOGGER.info("获取{}的子节点成功", path);

					resetServiceInfoByName((String) ctx, path, children);
					break;
				default:
					LOGGER.error("获取{}的子节点失败", path);
			}
		}, serviceName);
	}

	/**
	 * serviceName下子节点列表即可用服务地址列表
	 * 子节点命名为：host:port:versionName
	 *
	 * @param serviceName
	 * @param path
	 * @param infos
	 */
	private void resetServiceInfoByName(String serviceName, String path, List<String> infos) {
		LOGGER.info(serviceName + "\n" + infos);

		List<ServiceInfo> sinfos = new ArrayList<>();

		for (String info : infos) {
			String[] serviceInfo = info.split(":");
			ServiceInfo sinfo = new ServiceInfo(serviceInfo[0], Integer.valueOf(serviceInfo[1]), serviceInfo[2]);
			sinfos.add(sinfo);
		}

		if (caches.containsKey(serviceName)) {
			List<ServiceInfo> currentInfos = caches.get(serviceName);

			for (ServiceInfo sinfo : sinfos) {
				for (ServiceInfo currentSinfo : currentInfos) {
					if (sinfo.equalTo(currentSinfo)) {
						sinfo.setActiveCount(currentSinfo.getActiveCount());
						break;
					}
				}
			}
		}
		caches.put(serviceName, sinfos);
		apiServices.reloadServices();
	}

	/**
	 * 获取可用服务 IP + Port + Version
	 *
	 * @param
	 */
	public static Map getAvailableServices() {
		Map<String, List<ServiceInfo>> availableServices = new ConcurrentHashMap<>();
		Set<String> servicesKey = caches.keySet();
		for (String key : servicesKey) {
			List<ServiceInfo> serviceInfos = caches.get(key);
			if (serviceInfos.size() > 0) {
				availableServices.put(key, serviceInfos);
			}
		}
		return availableServices;
	}


	//----------------------servicesInfo相关-----------------------------------

	//----------------------static config-------------------------------------
	private void getConfig(String path) {


		//每次getConfig之前，先判断父节点是否存在，若不存在，则创建
		tryCreateNode("/soa/config");

		zk.getChildren(path, watchedEvent -> {
			if (watchedEvent.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
				LOGGER.info(watchedEvent.getPath() + "'s children changed, reset config in memory");

				getConfig(watchedEvent.getPath());
			}
		}, (rc, path1, ctx, children) -> {
			switch (KeeperException.Code.get(rc)) {
				case CONNECTIONLOSS:
					LOGGER.info("connect loss, reset {} config in memory", path1);

					getConfig(path1);
					break;
				case OK:
					LOGGER.info("get children of {} succeed.", path1);

					resetConfigCache(path1, children);

					break;
				default:
					LOGGER.error("get chileren of {} failed", path1);
			}
		}, null);
	}

	private void resetConfigCache(String path, List<String> children) {
		for (String key : children) {
			String configNodePath = path + "/" + key;

			getConfigData(configNodePath, key);
		}
	}

	private void getConfigData(String path, String configNodeName) {
		if (configNodeName == null) {
			String[] tmp = path.split("/");
			configNodeName = tmp[tmp.length - 1];
		}

		zk.getData(path, watchedEvent -> {
			if (watchedEvent.getType() == Watcher.Event.EventType.NodeDataChanged) {
				LOGGER.info(watchedEvent.getPath() + "'s data changed, reset config in memory");
				getConfigData(watchedEvent.getPath(), null);
			}
		}, (rc, path1, ctx, data, stat) -> {
			switch (KeeperException.Code.get(rc)) {
				case CONNECTIONLOSS:
					getConfigData(path1, (String) ctx);
					break;
				case OK:
					processConfigData((String) ctx, data);
					break;
				default:
					LOGGER.error("Error when trying to get data of {}.", path1);
			}
		}, configNodeName);
	}

	private void processConfigData(String configNode, byte[] data) {
		Map<ConfigKey, Object> propertyMap = new HashMap<>();
		try {
			String propertiesStr = new String(data, "utf-8");

			String[] properties = propertiesStr.split(";");
			for (String property : properties) {

				String[] key_values = property.split("=");
				if (key_values.length == 2) {

					ConfigKey type = ConfigKey.findByValue(key_values[0]);
					switch (type) {

						case Thread:
							Integer value = Integer.valueOf(key_values[1]);
							propertyMap.put(type, value);
							break;
						case ThreadPool:
							Boolean bool = Boolean.valueOf(key_values[1]);
							propertyMap.put(type, bool);
							break;
						case Timeout:
							Integer timeout = Integer.valueOf(key_values[1]);
							propertyMap.put(type, timeout);
							break;
						case LoadBalance:
							propertyMap.put(type, key_values[1]);
							break;
						case FailOver:
							propertyMap.put(type, Integer.valueOf(key_values[1]));
							break;
						case Compatible:
							propertyMap.put(type, key_values[1].split(","));
							break;
					}
				}
			}

			LOGGER.info("get config form {} with data [{}]", configNode, propertiesStr);
		} catch (UnsupportedEncodingException e) {
			LOGGER.error(e.getMessage(), e);
		}

		config.put(configNode, propertyMap);
	}

	//---------------------static config end-----------------------------------

	/**
	 * 连接zookeeper
	 */
	private void connect() {
		try {
			connectDownLatch = new CountDownLatch(1);

			zk = new ZooKeeper(SoaSystemEnvProperties.SOA_ZOOKEEPER_HOST, 15000, e -> {
				if (e.getState() == Watcher.Event.KeeperState.Expired) {
					LOGGER.info("{} 到zookeeper Server的session过期，重连", isClient ? "Client's" : "Server's");

					destroy();

					init();
				} else if (e.getState() == Watcher.Event.KeeperState.SyncConnected) {
					LOGGER.info("{} Zookeeper Watcher 已连接 zookeeper Server", isClient ? "Client's" : "Server's");
//                    connectDownLatch.countDown();
				}
			});
		} catch (Exception e) {
			LOGGER.info(e.getMessage(), e);
		}
	}

	public Map<String, Map<ConfigKey, Object>> getConfig() {
		return config;
	}
}
