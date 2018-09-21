package gaia.cu9.ari.gaiaorbit.analytics;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.http.NameValuePair;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files;
import com.badlogic.gdx.utils.TimeUtils;
import com.brsanthu.googleanalytics.GoogleAnalytics;
import com.brsanthu.googleanalytics.GoogleAnalyticsResponse;
import com.brsanthu.googleanalytics.PageViewHit;
import com.brsanthu.googleanalytics.TimingHit;

import gaia.cu9.ari.gaiaorbit.desktop.format.DesktopDateFormatFactory;
import gaia.cu9.ari.gaiaorbit.desktop.util.DesktopConfInit;
import gaia.cu9.ari.gaiaorbit.util.ConfInit;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.format.DateFormatFactory;

public class AnalyticsReporting {

	private static long startTimeMs;

	private static AnalyticsReporting instance;

	public static void initialize(IPermission permission) {
		if (instance == null)
			instance = new AnalyticsReporting(permission);
	}

	public static AnalyticsReporting getInstance() {
		initialize(null);
		return instance;
	}

	private final GoogleAnalytics ga;
	private final String appname, languagetag, version;
	private IPermission permission;

	private AnalyticsReporting(IPermission permission) {
		if (permission.check())
			this.ga = new GoogleAnalytics("UA-57580474-6");
		else
			this.ga = null;
		this.appname = GlobalConf.APPLICATION_NAME;
		this.languagetag = I18n.locale.toLanguageTag().toLowerCase();
		this.version = GlobalConf.version.version + "." + GlobalConf.version.build;
		this.permission = permission;
	}

	public Future<GoogleAnalyticsResponse> sendStartAppReport() {
		if (!permission.check())
			return null;
		PageViewHit pvh = new PageViewHit("http://gaiasky.com", appname + " start");
		pvh.applicationName(appname);
		pvh.applicationVersion(version);
		pvh.userLanguage(languagetag);
		pvh.documentPath("/start");
		startTimeMs = TimeUtils.millis();
		return ga.postAsync(pvh);
	}

	public Future<GoogleAnalyticsResponse> sendTimingAppReport() {
		if (!permission.check())
			return null;
		TimingHit th = new TimingHit();
		th.applicationName(appname);
		th.applicationVersion(version);
		th.userLanguage(languagetag);
		th.pageLoadTime((int) (TimeUtils.millis() - startTimeMs));
		return ga.postAsync(th);
	}

	public void printResponse(GoogleAnalyticsResponse gar) {
		System.out.println("Response status code: " + gar.getStatusCode());
		List<NameValuePair> result = gar.getPostedParms();
		if (result != null)
			for (NameValuePair pair : result) {
				System.out.println(pair.toString());
			}
		else
			System.out.println("No params");
	}

	public static void main(String[] args) throws Exception {

		// INITIALISE PARAMS
		Gdx.files = new Lwjgl3Files();
		DateFormatFactory.initialize(new DesktopDateFormatFactory());
		ConfInit.initialize(new DesktopConfInit());
		I18n.initialize(Gdx.files.absolute(GlobalConf.ASSETS_LOC + "/i18n/gsbundle"));

		AnalyticsReporting.initialize(new IPermission() {
			@Override
			public boolean check() {
				return true;
			}
		});
		AnalyticsReporting ar = AnalyticsReporting.getInstance();

		// REQUESTS
		Future<GoogleAnalyticsResponse> fstart = ar.sendStartAppReport();
		try {
			GoogleAnalyticsResponse gar = fstart.get(15, TimeUnit.SECONDS);
			ar.printResponse(gar);
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println();
		Thread.sleep(2000);

		Future<GoogleAnalyticsResponse> ftiming = ar.sendTimingAppReport();
		try {
			GoogleAnalyticsResponse gar = ftiming.get(15, TimeUnit.SECONDS);
			ar.printResponse(gar);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
