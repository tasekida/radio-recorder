/**
 * NHK第2放送を録音
 */
module radio.recorder {
	exports cyou.obliquerays.logging;
	exports cyou.obliquerays.media;
	exports cyou.obliquerays.media.downloader;
	exports cyou.obliquerays.media.downloader.model;

	requires transitive java.logging;
	requires java.net.http;
	requires jave.core;
	requires jdk.httpserver;
	requires java.xml;
}