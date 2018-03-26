package com.github.dapeng.impl.plugins.deploy_agent;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.cache.ResolutionCacheManager;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorWriter;
import org.apache.ivy.plugins.report.XmlReportParser;
import org.apache.ivy.util.filter.FilterHelper;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.text.ParseException;
import java.util.*;

/**
 * 
 * @author xiaopeng.he
 * 
 */
public final class IvyGetJarUtil {

	private Ivy ivy;

	public IvyGetJarUtil(Ivy ivy) {
		this.ivy = ivy;
	}

	public Ivy getIvy() {
		return ivy;
	}

	public void setIvy(Ivy ivy) {
		this.ivy = ivy;
	}

	private File getIvyfile(String organisation, String name, String revision, String[] confs) throws IOException {
		File ivyfile;
		ivyfile = File.createTempFile("ivy", ".xml");
		ivyfile.deleteOnExit();
		DefaultModuleDescriptor md = DefaultModuleDescriptor.newDefaultInstance(ModuleRevisionId.newInstance(
				organisation, name + "-caller", "working"));
		DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(md, ModuleRevisionId.newInstance(organisation,
				name, revision), false, false, true);
		for (int i = 0; i < confs.length; i++) {
			dd.addDependencyConfiguration("default", confs[i]);
		}
		md.addDependency(dd);
		XmlModuleDescriptorWriter.write(md, ivyfile);
		return ivyfile;
	}

	private ResolveReport resolve(File ivyfile, String[] confs) throws MalformedURLException, ParseException,
			IOException {
		ResolveOptions resolveOptions = new ResolveOptions().setConfs(confs).setValidate(true).setResolveMode(null)
				.setArtifactFilter(FilterHelper.getArtifactTypeFilter("jar,bundle"));
		return ivy.resolve(ivyfile.toURI().toURL(), resolveOptions);
	}

	private Set<URI> getCachePath(ModuleDescriptor md, String[] confs) {
		Set<URI> fs = new HashSet<URI>();
		try {
			String pathSeparator = System.getProperty("path.separator");
			StringBuilder buf = new StringBuilder();
			Collection<ArtifactDownloadReport> all = new LinkedHashSet<ArtifactDownloadReport>();
			ResolutionCacheManager cacheMgr = ivy.getResolutionCacheManager();
			XmlReportParser parser = new XmlReportParser();
			for (int i = 0; i < confs.length; i++) {
				String resolveId = ResolveOptions.getDefaultResolveId(md);
				File report = cacheMgr.getConfigurationResolveReportInCache(resolveId, confs[i]);
				parser.parse(report);
				all.addAll(Arrays.asList(parser.getArtifactReports()));
			}
			for (ArtifactDownloadReport artifact : all) {
				if (artifact.getLocalFile() != null) {
					buf.append(artifact.getLocalFile().getCanonicalPath());
					buf.append(pathSeparator);
				}
			}
			String[] fs_str = buf.toString().split(pathSeparator);
			for (String str : fs_str) {
				File file = new File(str);
				if (file.exists()) {
					fs.add(file.toURI());
				}
			}
		} catch (Exception ex) {
			throw new RuntimeException("impossible to build ivy cache path: " + ex.getMessage(), ex);
		}
		return fs;
	}

	public Set<URI> getJars(String organisation, String name, String revision, String[] confs) throws Exception {
		Set<URI> jars = new HashSet<URI>();
		try {
			ivy.getSettings().addAllVariables(System.getProperties());
			ivy.pushContext();
			String[] confs2 = new String[] { "default" };
			File ivyfile = getIvyfile(organisation, name, revision, confs);
			ResolveReport report = resolve(ivyfile, confs2);
			if (report.hasError()) {
				List<?> problemMessages = report.getAllProblemMessages();
				for (Object message : problemMessages) {
					throw new Exception(message.toString());
				}
			} else {
				jars.addAll(getCachePath(report.getModuleDescriptor(), confs2));
			}
		} finally {
			ivy.popContext();
		}
		return jars;
	}
}
