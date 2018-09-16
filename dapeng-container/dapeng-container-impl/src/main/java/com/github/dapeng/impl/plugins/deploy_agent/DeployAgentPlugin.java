package com.github.dapeng.impl.plugins.deploy_agent;

import com.github.dapeng.api.Plugin;
import com.github.dapeng.bootstrap.classloader.ApplicationClassLoader;
import com.github.dapeng.bootstrap.classloader.ClassLoaderFactory;
import com.github.dapeng.core.helper.SoaSystemEnvProperties;
import org.apache.ivy.Ivy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DeployAgentPlugin implements Plugin{

    private static final Logger LOGGER = LoggerFactory.getLogger(DeployAgentPlugin.class);

    private final List<ClassLoader> appClassLoaders;

    public DeployAgentPlugin(List<ClassLoader> appsCls) {
        this.appClassLoaders = appsCls;
    }

    @Override
    public void start() {
        LOGGER.info(" check if use ivy to deploy apps:  " + SoaSystemEnvProperties.SOA_USE_IVY);
        if (SoaSystemEnvProperties.SOA_USE_IVY) {
            try {
                String organisation = SoaSystemEnvProperties.SOA_ORGANIZATION;
                if (StringUtils.isEmpty(organisation)) {
                    throw new Exception(" use ivy deploy must defined organization => soa.organization");
                }

                String serviceArtifactId = SoaSystemEnvProperties.SOA_ARTIFACT_ID;
                if (StringUtils.isEmpty(serviceArtifactId)) {
                    throw new Exception(" use ivy deploy must defined serviceArtifactId => soa.artifact.id");
                }

                String serviceVersion = SoaSystemEnvProperties.SOA_SERVICE_VERSION;
                if (StringUtils.isEmpty(serviceVersion)) {
                    throw new Exception(" use ivy deploy must defined serviceVersion => soa.service.version");
                }

                LOGGER.info(" start to load " + organisation + "::" + serviceVersion + "::" + serviceArtifactId);

                String targetDir = SoaSystemEnvProperties.SOA_IVY_JARS_DIR;
                LOGGER.info(" use " + targetDir + " as apps jars dir..........");

                Set<URI> serviceUris = loadLibrary(organisation, serviceVersion, serviceArtifactId);

                Iterator<URI> iterator = serviceUris.iterator();
                while (iterator.hasNext()) {
                    URI uri = iterator.next();
                    if (!new File(targetDir + "/" + uri.getPath().substring(uri.getPath().lastIndexOf("/") + 1)).exists()) {
                        writeFile(uri.toURL(),uri.getPath().substring(uri.getPath().lastIndexOf("/") + 1),targetDir );
                    } else {
                        LOGGER.warn(" file: " + uri.getPath() + " already exists.....");
                    }
                }

                List<URL> appUrls = serviceUris.stream().map(i -> {
                    try {
                        return i.toURL();
                    } catch (Exception e) {
                        return null;
                    }
                }).filter(i -> i != null).collect(Collectors.toList());

                URL[] urls = new URL[(appUrls.size())];
                appUrls.toArray(urls);

                ApplicationClassLoader appLoader = new ApplicationClassLoader(urls, ClassLoaderFactory.getCoreClassLoader());
                appClassLoaders.add(appLoader);

            } catch (Exception e) {
                LOGGER.error(" failed to load service url........." + e.getMessage());
            }
        } else {
            LOGGER.warn(" Didn't use ivy to load apps...skip DeployAgentPlugin.........");
        }

    }

    @Override
    public void stop() {

    }

    /**
     * 将文件写入本地
     *
     * @param fromUrl  httpPath
     * @param targetFileName targetDirFile
     * @param targetDir targetDir
     */
    private void writeFile(URL fromUrl, String targetFileName, String targetDir)  {
        BufferedInputStream inputStream = null;
        BufferedOutputStream outputStream = null;
        try {
            inputStream = new BufferedInputStream(fromUrl.openStream());
            //inputStream = new BufferedInputStream(new FileInputStream(fromUrl));
            outputStream = new BufferedOutputStream(new FileOutputStream(new File(targetDir + "/" + targetFileName)));

            while (inputStream.available() > 0) {
                outputStream.write(inputStream.read());
            }
            //缓冲区的内容写入到文件
            outputStream.flush();

        } catch (MalformedURLException e) {
            LOGGER.error(" failed to get url from: " + fromUrl);
        } catch (IOException e) {
            LOGGER.error(" failed to open url: " +  fromUrl);
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (Exception e) {
                LOGGER.error(" failed to close stream......." + e.getMessage());
            }
        }

    }

    /**
     * 初始化ServiceClassloader的ClassPath
     * Platform & core classLoader的jar包包含在dapeng镜像中，不需要再加载
     */
    private Set<URI> loadLibrary(String organisation, String serviceVersion, String serviceArtifactId) throws Exception {
        Set<URI> serviceUrls = new HashSet<URI>();

        if (SoaSystemEnvProperties.SOA_USE_IVY) {
            IvyGetJarUtil ivyGetJarUtil = new IvyGetJarUtil(getIvy());

            if (serviceArtifactId != null) {
                String[] temp = serviceArtifactId.split(":");
                serviceUrls.addAll(
                        ivyGetJarUtil.getJars(organisation, temp[0], serviceVersion, new String[] { "master(*)", "compile(*)" }));
            }
        }

        return serviceUrls;
    }


    /**
     * list files in dir or file, filter by nameRegex.
     */
    private static Set<URI> listFilesUrls(File file, String nameRegex) {
        Set<URI> urlSet = new HashSet<>();

        if (file == null || !file.exists()) {
            return urlSet;
        }

        if (file.isFile() && file.getName().matches(nameRegex)) {
            urlSet.add(file.toURI());
        } else if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    urlSet.addAll(listFilesUrls(files[i], nameRegex));
                }
            }
        }
        return urlSet;
    }

    private Ivy getIvy() throws ParseException, IOException {
        Ivy ivy = Ivy.newInstance();

        File cacheFile = new File(System.getProperty("user.home") + "/.ivy2/cache");
        if (!cacheFile.exists()) {
            cacheFile.mkdirs();
        }
        ivy.getSettings().setDefaultCache(cacheFile);

        File localFile = new File(System.getProperty("user.home") + "/.m2/repository");
        if (!localFile.exists()) {
            localFile.mkdirs();
        }
        ivy.getSettings().setVariable("ivy.local.default.root", localFile.getAbsolutePath());

        LOGGER.info("IVY Plugin cache path: " + cacheFile.getAbsolutePath());
        LOGGER.info("IVY Plugin local path: " + localFile.getAbsolutePath());

        String ivySettingFile = SoaSystemEnvProperties.SOA_IVY_SETTING_FILE;
        URL ivySettingsURL = null;
        if (ivySettingFile == null || ivySettingFile.isEmpty()) {
            ivySettingsURL = DeployAgentPlugin.class.getClassLoader().getResource("ivysettings.xml");
        } else {
            ivySettingsURL = new File(ivySettingFile).toURI().toURL();
        }
        ivy.getSettings().load(ivySettingsURL);

        return ivy;
    }
}
