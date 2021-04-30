/**
 * NHK第2放送を録音
 */
module radio.recorder {
	exports cyou.obliquerays.media;
	exports cyou.obliquerays.logging;

	requires transitive java.logging;
	requires java.net.http;
}