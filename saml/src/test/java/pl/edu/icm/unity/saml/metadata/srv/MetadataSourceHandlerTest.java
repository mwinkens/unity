/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata.srv;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.IOUtils;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.junit.Before;
import org.junit.Test;

import pl.edu.icm.unity.base.file.FileData;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.exceptions.EngineException;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;

public class MetadataSourceHandlerTest
{
	private ExecutorsService executorsService;
	private RemoteMetadataSrc src;
	private FileStorageService fileStorageService;
	private URIAccessService uriAccessService;
	
	@Before
	public void init() throws EngineException, IOException, URISyntaxException
	{
		executorsService = mock(ExecutorsService.class);
		ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);
		when(executorsService.getService()).thenReturn(pool);
		
		src = new RemoteMetadataSrc("http://url", null);

		fileStorageService = mock(FileStorageService.class);
		uriAccessService = mock(URIAccessService.class);
		
		when(fileStorageService.readFileFromWorkspace(any())).thenThrow(EngineException.class);
		
		when(uriAccessService.readURI(eq(new URI("http://url")))).thenAnswer((a) -> new FileData("xx",
				Files.readAllBytes(Paths.get("src/test/resources/unity-as-sp-meta.xml")), new Date()));
		
		when(uriAccessService.readURI(eq(new URI("http://url")), any())).thenAnswer((a) -> new FileData("xx",
				Files.readAllBytes(Paths.get("src/test/resources/unity-as-sp-meta.xml")), new Date()));
		
		when(fileStorageService.storeFileInWorkspace(any(), any())).thenAnswer((a) -> new FileData("xx",
				Files.readAllBytes(Paths.get("src/test/resources/unity-as-sp-meta.xml")), new Date()));
	}
	
	@Test
	public void shouldNotifyFirstConsumer() throws Exception
	{
		MetadataDownloader downloader = new MetadataDownloader(uriAccessService, fileStorageService);
		MetadataSourceHandler handler = new MetadataSourceHandler(src, 
				executorsService, downloader, 15);
		
		AtomicBoolean gotEvent = new AtomicBoolean(false);
		MetadataConsumer consumer = new MetadataConsumer(1500, (m,id) -> gotEvent.set(true), "1");
		handler.addConsumer(consumer);
		
		Awaitility.await().atMost(Duration.ONE_SECOND).until(
				() -> gotEvent.get());
		verify(uriAccessService).readURI(eq(new URI("http://url")), any());
	}
	
	@Test
	public void shouldNotify2Consumers() throws Exception
	{
		MetadataDownloader downloader = new MetadataDownloader(uriAccessService, fileStorageService);
		MetadataSourceHandler handler = new MetadataSourceHandler(src, 
				executorsService, downloader, 15);
		
		AtomicBoolean event1 = new AtomicBoolean(false);
		MetadataConsumer consumer1 = new MetadataConsumer(1500, (m,id) -> event1.set(true), "1");
		handler.addConsumer(consumer1);
		AtomicBoolean event2 = new AtomicBoolean(false);
		MetadataConsumer consumer2 = new MetadataConsumer(1500, (m,id) -> event2.set(true), "2");
		handler.addConsumer(consumer2);
		
		Awaitility.await().atMost(Duration.ONE_SECOND).until(
				() -> event1.get() && event2.get());
		verify(uriAccessService, atMost(2)).readURI(new URI("http://url"));
	}

	@Test
	public void shouldNotNotifyDeregisteredConsumer() throws Exception
	{
		MetadataDownloader downloader = new MetadataDownloader(uriAccessService, fileStorageService);
		MetadataSourceHandler handler = new MetadataSourceHandler(src, 
				executorsService, downloader, 15);
		
		MetadataConsumer consumer1 = new MetadataConsumer(1500, (m,id) -> {}, "1");
		handler.addConsumer(consumer1);
		AtomicInteger event = new AtomicInteger(0);
		MetadataConsumer consumer2 = new MetadataConsumer(100, 
				(m,id) -> {handler.removeConsumer("2"); event.incrementAndGet();},
				"2");
		handler.addConsumer(consumer2);
		
		Thread.sleep(400);
		verify(uriAccessService, atMost(2)).readURI(new URI("http://url"));
		assertThat(event.get(), is(1));
	}
	
	@Test
	public void shouldStopRefreshAfterDeregistrationOfLastConsumer() throws Exception
	{
		MetadataDownloader downloader = mock(MetadataDownloader.class);
		when(downloader.getFresh("http://url", null)).thenAnswer((a) -> {
			String xml = IOUtils.toString(new FileInputStream("src/test/resources/unity-as-sp-meta.xml"),
					StandardCharsets.UTF_8);
			return EntitiesDescriptorDocument.Factory.parse(xml);
		});
		
		MetadataSourceHandler handler = new MetadataSourceHandler(src, 
				executorsService, downloader, 20);
		
		AtomicInteger invCount = new AtomicInteger(0);
		MetadataConsumer consumer1 = new MetadataConsumer(20, (m,id) -> invCount.incrementAndGet(), "1");
		handler.addConsumer(consumer1);
		handler.removeConsumer("1");
		int inv = invCount.get()+1;
		Thread.sleep(100);
		verify(downloader, atMost(inv)).getFresh("http://url", null);
	}
	
	@Test
	public void shouldStartNotificationsAfterStopping() throws Exception
	{
		MetadataDownloader downloader = new MetadataDownloader(uriAccessService, fileStorageService);
		MetadataSourceHandler handler = new MetadataSourceHandler(src, 
				executorsService, downloader, 15);
		
		MetadataConsumer consumer1 = new MetadataConsumer(1500, (m,id) -> {}, "1");
		handler.addConsumer(consumer1);
		handler.removeConsumer("1");
		
		AtomicBoolean gotEvent = new AtomicBoolean(false);
		MetadataConsumer consumer2 = new MetadataConsumer(1500, (m,id) -> gotEvent.set(true), "2");
		handler.addConsumer(consumer2);
		
		Awaitility.await().atMost(Duration.ONE_SECOND).until(
				() -> gotEvent.get());
		verify(uriAccessService, atLeast(1)).readURI(eq(new URI("http://url")), any());
	}
	
	@Test
	public void shouldUseLowestNotificationIntervalAfterRegistration() throws Exception
	{
		MetadataDownloader downloader = new MetadataDownloader(uriAccessService, fileStorageService);
		MetadataSourceHandler handler = new MetadataSourceHandler(src, 
				executorsService, downloader, 1000);
		
		MetadataConsumer consumer1 = new MetadataConsumer(15000, (m,id) -> {}, "1");
		handler.addConsumer(consumer1);
		assertThat(handler.getRefreshInterval(), is(15000L));

		MetadataConsumer consumer2 = new MetadataConsumer(5000, (m,id) -> {}, "2");
		handler.addConsumer(consumer2);
		assertThat(handler.getRefreshInterval(), is(5000L));
	}
	
	@Test
	public void shouldUseLowestNotificationIntervalAfterDeregistration() throws Exception
	{
		MetadataDownloader downloader = new MetadataDownloader(uriAccessService, fileStorageService);
		MetadataSourceHandler handler = new MetadataSourceHandler(src, 
				executorsService, downloader, 1000);
		
		MetadataConsumer consumer1 = new MetadataConsumer(15000, (m,id) -> {}, "1");
		handler.addConsumer(consumer1);
		assertThat(handler.getRefreshInterval(), is(15000L));

		MetadataConsumer consumer2 = new MetadataConsumer(5000, (m,id) -> {}, "2");
		handler.addConsumer(consumer2);
		assertThat(handler.getRefreshInterval(), is(5000L));
		
		handler.removeConsumer("2");
		assertThat(handler.getRefreshInterval(), is(15000L));
	}
	
	@Test
	public void shouldImmediatellyNotifyWithCachedDataAfterRegistration() throws Exception
	{
		MetadataDownloader downloader = new MetadataDownloader(uriAccessService, fileStorageService);
		MetadataSourceHandler handler = new MetadataSourceHandler(src, 
				executorsService, downloader, 10000);
		
		AtomicBoolean gotEvent = new AtomicBoolean(false);
		MetadataConsumer consumer1 = new MetadataConsumer(15000, (m,id) -> gotEvent.set(true), "1");
		handler.addConsumer(consumer1);
		Awaitility.await().atMost(Duration.ONE_SECOND).until(
				() -> gotEvent.get());

		
		AtomicBoolean gotEvent2 = new AtomicBoolean(false);
		MetadataConsumer consumer2 = new MetadataConsumer(15000, (m,id) -> gotEvent2.set(true), "2");
		handler.addConsumer(consumer2);
		Awaitility.await().pollDelay(10, TimeUnit.MILLISECONDS)
				.atMost(Duration.ONE_SECOND).until(
				() -> gotEvent2.get());
	}
}
