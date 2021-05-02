/**
 * NHK第2放送を録音
 */
module radio.recorder {
	exports cyou.obliquerays.media;
	opens cyou.obliquerays.logging to java.logging;

	requires transitive java.logging;
	requires java.net.http;
}