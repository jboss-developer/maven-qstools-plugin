package org.jboss.maven.plugins.qstools.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Proxy;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.jboss.maven.plugins.qstools.Constants;

@Component(role = Resources.class)
public class Resources {

    @Requirement
    private ConfigurationProvider configurationProvider;

    @Requirement
    private Context context;

    private MavenSession mavenSession;

    private Log log;

    private void configure() throws ContextException {
        log = (Log) context.get(Constants.LOG_CONTEXT);
        mavenSession = (MavenSession) context.get(Constants.MAVEN_SESSION_CONTEXT);
    }

    public String getRedHatRepositorySnippet(String groupId) throws MalformedURLException, Exception {
        String location = configurationProvider.getQuickstartsRules(groupId).getRedHatMavenProfileSnippetLocation();
        InputStream is = getFileInputStream(new URL(location));
        String line = null;
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String ls = System.getProperty("line.separator");
        while ((line = br.readLine()) != null) {
            sb.append(line);
            sb.append(ls);
        }

        return sb.toString();
    }

    /**
     * Return a FileInputStream from a local file.
     * 
     * The local file is cached based on {@link Constants#CACHE_EXPIRES_SECONDS}
     * 
     * If the caches expires, them the file is downloaded again
     * 
     * @return
     * @throws FileNotFoundException
     * @throws ContextException 
     * 
     */
    public InputStream getFileInputStream(URL url) throws FileNotFoundException, ContextException {
        configure();
        InputStream repoStream = getCachedRepoStream(false, url);
        // if cache expired
        if (repoStream == null) {
            log.debug("Local cache file " + getLocalCacheFile(url) + " doesn't exist or cache has been expired");
            try {
                log.debug("Retrieving File from Remote repository " + url);
                repoStream = retrieveFileFromRemoteRepository(url);
                setCachedRepoStream(repoStream, url);
                log.debug("Forcing the use of local cache after download file without error from " + url);
                repoStream = getCachedRepoStream(true, url);
            } catch (Exception e) {
                log.warn("It was not possible to contact the repository at " + url + " . Cause " + e.getMessage());
                log.warn("Falling back to cache!");
                repoStream = getCachedRepoStream(true, url);
            }
        }
        return repoStream;
    }

    private InputStream getCachedRepoStream(final boolean force, URL url) throws FileNotFoundException {
        final String logmessage = "Local file %1s %2s used! Reason: Force:[%3b] - LastModification: %4d/%5d";
        File localCacheFile = getLocalCacheFile(url);
        if (localCacheFile.exists()) {
            long cachedvalidity = 1000 * Constants.CACHE_EXPIRES_SECONDS;
            long lastModified = localCacheFile.lastModified();
            long timeSinceLastModification = System.currentTimeMillis() - lastModified;
            // if online, consider the cache valid until it expires
            if (force || timeSinceLastModification <= cachedvalidity) {
                log.debug(String.format(logmessage, localCacheFile, "was", force, timeSinceLastModification,
                    cachedvalidity));
                return new FileInputStream(localCacheFile);
            }
            log.debug(String.format(logmessage, localCacheFile, "was not", force, timeSinceLastModification,
                cachedvalidity));
        }
        return null;
    }

    private void setCachedRepoStream(final InputStream stream, URL url) throws IOException {
        File localCacheFile = getLocalCacheFile(url);
        log.debug("Content stored at " + localCacheFile);
        if (!localCacheFile.exists()) {
            localCacheFile.createNewFile();
        }
        FileOutputStream fos = new FileOutputStream(localCacheFile);

        int i = 0;
        while ((i = stream.read()) != -1) {
            fos.write(i);
        }
        fos.close();
    }

    private File getLocalCacheFile(URL url) {
        // Remove no word character from the repo url
        String repo = url.toString().replaceAll("[^a-zA-Z_0-9]", "");
        return new File(System.getProperty("java.io.tmpdir"), repo);
    }

    private InputStream retrieveFileFromRemoteRepository(URL url) throws Exception {
        if (url.getProtocol().startsWith("http")) {
            HttpGet httpGet = new HttpGet(url.toURI());
            DefaultHttpClient client = new DefaultHttpClient();
            configureProxy(client);
            HttpResponse httpResponse = client.execute(httpGet);
            switch (httpResponse.getStatusLine().getStatusCode()) {
                case 200:
                    log.debug("Connected to repository! Getting " + url);
                    break;

                case 404:
                    log.error("Failed! (File not found: " + url + ")");
                    return null;

                default:
                    log.error("Failed! (server returned status code: "
                        + httpResponse.getStatusLine().getStatusCode());
                    return null;
            }
            log.info("Downloading " + url);
            return httpResponse.getEntity().getContent();
        } else if (url.getProtocol().startsWith("file")) {
            return new FileInputStream(new File(url.toURI()));
        }
        return null;
    }

    private void configureProxy(DefaultHttpClient client) {
        Proxy proxyConfig = null;
        if (mavenSession.getSettings().getProxies().size() > 0) {
            proxyConfig = mavenSession.getSettings().getProxies().get(0);
        }
        if (proxyConfig != null) {
            HttpHost proxy = new HttpHost(proxyConfig.getHost(), proxyConfig.getPort());
            client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
            String proxyUsername = proxyConfig.getUsername();
            if (proxyUsername != null && !proxyUsername.isEmpty()) {
                String proxyPassword = proxyConfig.getPassword();
                AuthScope authScope = new AuthScope(proxyConfig.getHost(), proxyConfig.getPort());
                UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(proxyUsername, proxyPassword);
                client.getCredentialsProvider().setCredentials(authScope, credentials);
            }
        }
    }
}
