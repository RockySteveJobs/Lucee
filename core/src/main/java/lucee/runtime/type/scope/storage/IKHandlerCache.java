package lucee.runtime.type.scope.storage;

import java.io.IOException;

import lucee.commons.collection.MapPro;
import lucee.commons.io.cache.Cache;
import lucee.commons.io.log.Log;
import lucee.runtime.PageContext;
import lucee.runtime.cache.CacheConnection;
import lucee.runtime.cache.CacheUtil;
import lucee.runtime.engine.ThreadLocalPageContext;
import lucee.runtime.exp.ApplicationException;
import lucee.runtime.exp.PageException;
import lucee.runtime.op.Caster;
import lucee.runtime.type.Collection;
import lucee.runtime.type.scope.ScopeContext;

public class IKHandlerCache implements IKHandler {
	@Override
	public IKStorageValue loadData(PageContext pc, String appName, String name, String strType, 
			int type, Log log) throws PageException	{
		Cache cache = getCache(pc,name);
		String key=getKey(pc.getCFID(),appName,strType);
		Object val = cache.getValue(key,null);

		if(val instanceof IKStorageValue) {
			ScopeContext.info(log,"load existing data from  cache ["+name+"] to create "+strType+" scope for "+pc.getApplicationContext().getName()+"/"+pc.getCFID());
			return (IKStorageValue)val;
		}
		else {
			ScopeContext.info(log,"create new "+strType+" scope for "+pc.getApplicationContext().getName()+"/"+pc.getCFID()+" in cache ["+name+"]");
		}
		return null;
	}
	
	@Override
	public synchronized void store(IKStorageScopeSupport storageScope, PageContext pc, String appName, String name, String cfid,
			MapPro<Collection.Key,IKStorageScopeItem> data, Log log) {
		try {
			Cache cache = getCache(ThreadLocalPageContext.get(pc), name);
			String key=getKey(cfid, appName, storageScope.getTypeAsString());
			
			Object existingVal = cache.getValue(key,null);
			cache.put(
					key, 
					new IKStorageValue(IKStorageScopeSupport.prepareToStore(data,existingVal,storageScope.lastModified())),
					new Long(storageScope.getTimeSpan()), null);
		} 
		catch (Exception pe) {pe.printStackTrace();}
	}
	
	@Override
	public synchronized void unstore(IKStorageScopeSupport storageScope, PageContext pc, String appName, String name, String cfid, Log log) {
		try {
			Cache cache = getCache(pc, name);
			String key=getKey(cfid, appName, storageScope.getTypeAsString());
			cache.remove(key);
		} 
		catch (Exception pe) {}
	}
	
	

	private static Cache getCache(PageContext pc, String cacheName) throws PageException {
		try {
			CacheConnection cc = CacheUtil.getCacheConnection(pc,cacheName);
			if(!cc.isStorage()) 
				throw new ApplicationException("storage usage for this cache is disabled, you can enable this in the Lucee administrator.");
			return CacheUtil.getInstance(cc,ThreadLocalPageContext.getConfig(pc)); //cc.getInstance(config); 
		} catch (IOException e) {
			throw Caster.toPageException(e);
		}
	}

	private static String getKey(String cfid, String appName, String type) {
		return new StringBuilder("lucee-storage:").append(type).append(":").append(cfid).append(":").append(appName).toString().toUpperCase();
	}

	@Override
	public String getType() {
		return "Cache";
	}
}