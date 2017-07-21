package org.opensubtitles.api;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.vuze.mediaplayer.ISO639;

import redstone.xmlrpc.XmlRpcArray;
import redstone.xmlrpc.XmlRpcProxy;

public class OSXMLRPC {
	
	static interface OpenSubtitles {
	
		public Map<String,String> ServerInfo();
		public Map<String,String> LogIn(String username,String password,String language,String useragent);
		public Map<String,String> LogOut(String token);
		public Map			  SearchSubtitles(String token, Map<String,String>[] queries); 
		public Map			  TryUploadSubtitles(String token,Map queries);
		public Map			  UploadSubtitles(String token,Map queries);
	
	}
	
	
	public static String logIn(String username,String password) {
		try {
			OpenSubtitles os = createOS();
			Map<String,String> sResult = os.LogIn(username, password, "en", "Vuze Player");
			
			return sResult.get("token");
		} catch (Exception e) {
			// TODO: handle exception
		}
		return null;
	}
	
	public static UploadInfo getUploadInfo(String token,String subHash,String subFileName,String movieHash,long movieSize,String movieFileName) {
		UploadInfo info = new UploadInfo();
		try{
			
			OpenSubtitles os = createOS();
			Map<String,Object> subQuery = new HashMap<String,Object>(5);
			subQuery.put("subhash", subHash.toLowerCase());
			subQuery.put("subfilename", subFileName);
			subQuery.put("moviehash", movieHash);
			subQuery.put("moviebytesize", new Long(movieSize));
			subQuery.put("moviefilename", movieFileName);
			
			Map<String,Object> query = new HashMap<String, Object>(1);
			query.put("cd1", subQuery);
			
			Map sResult = os.TryUploadSubtitles("", query );
			
			//System.out.println(sResult);
			
			int alreadyInDb = ((Integer) sResult.get("alreadyindb")).intValue();
			
			info.setShouldUpload(alreadyInDb == 0);
			Map response = null;
			
			try {
				XmlRpcArray data = (XmlRpcArray) sResult.get("data");
				response = (Map) data.get(0);
			} catch (Exception e) {
				try {
					response = (Map) sResult.get("data");
				} catch (Exception ex) {
					//fail ...
				}
			}
			
			if(response != null) {
				info.setImdbId((String)response.get("IDMovieImdb"));
			}
			
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		return info;
	}
	
	public static void uploadSubtitles(String token,String subHash,String subFileName,String movieHash,long movieSize,String movieFileName,String subContent,String imdbId,Locale language) {
		try{
			OpenSubtitles os = createOS();
			Map<String,Object> subQuery = new HashMap<String,Object>(5);
			subQuery.put("subhash", subHash.toLowerCase());
			subQuery.put("subfilename", subFileName);
			subQuery.put("moviehash", movieHash);
			subQuery.put("moviebytesize", new Long(movieSize));
			subQuery.put("moviefilename", movieFileName);
			subQuery.put("subcontent", subContent);
			
			Map<String,Object> data = new HashMap<String, Object>();
			
			data.put("cd1", subQuery);
			
			Map<String,String> baseInfo = new HashMap<String,String>();
			baseInfo.put("idmovieimdb", imdbId);
			baseInfo.put("sublanguageid", ISO639.getISO639_2FromLocale(language));
			data.put("baseinfo", baseInfo);
			
			
			Map<String,Object> query = new HashMap<String, Object>(1);
			query.put("data", data);
			
			
			Map sResult = os.UploadSubtitles(token, data );
			
			System.out.println("Status : " + sResult.get("status"));
			System.out.println("data   : " +sResult.get("data"));
			
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public static Subtitle[] searchSubtitles(String hash,long size) {
		try {
			OpenSubtitles os = createOS();
			
			Map<String,String> query = new HashMap<String, String>();
			query.put("sublanguageid", "all");
			query.put("moviehash", hash);
			query.put("moviebytesize", "" + size);
			
			Map[] queries = new Map[1];
			queries[0] = query;
			
			Map sResult = os.SearchSubtitles("", queries );
			
			List subtitles = null;
			try {
			
				subtitles = (List) sResult.get("data");
			} catch (Exception e) {
				
			}
			
			if(subtitles == null) {
				return new Subtitle[0];
			}
			
			List<Subtitle> results = new ArrayList<Subtitle>(subtitles.size());
			
			for(int i = 0 ; i < subtitles.size() ; i++) {
				Map<String,String> sub = (Map<String,String>) subtitles.get(i);
				String downloadLink = sub.get("SubDownloadLink");
				String isoCode =  sub.get("ISO639");
				String fileExtension = sub.get("SubFormat");
				if(downloadLink != null && isoCode != null) {
					Subtitle subtitle = new Subtitle(downloadLink,isoCode,fileExtension);
					subtitle.setScore(sub.get("SubRating"));
					subtitle.setNbDownloads(sub.get("SubDownloadsCnt"));
					results.add(subtitle);
				}
				
			}
			
			return results.toArray(new Subtitle[results.size()]);
			
		} catch (Exception e) {
			e.printStackTrace();
			return new Subtitle[0];
		}
	}

	private static OpenSubtitles createOS() throws MalformedURLException {
		Map<String,String> properties = new HashMap<String, String>();
		properties.put("User-Agent","Vuze Player");
		OpenSubtitles os = ( OpenSubtitles ) XmlRpcProxy.createProxy( new URL("http://api.opensubtitles.org/xml-rpc"),null, new Class[] { OpenSubtitles.class },false,properties );
		return os;
	}
	
	public static void main(String[] args) throws Exception{
		//XmlRpcClient client = new XmlRpcClient("http://api.opensubtitles.org/xml-rpc",false);
		//Map<String,String> result = (Map<String,String>) client.invoke("ServerInfo", new Object[0]);
		//System.out.println(result);
		
		Subtitle[] results = searchSubtitles("C529A3FC3FF5930E", 366702592l);
		System.out.println(results);
		
		
		
	}

}
