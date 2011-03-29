/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 * 
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.web.ws.provider;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;

//@Component
public class ProtobufProviderHelper {

  private static final Logger log = LoggerFactory.getLogger(ProtobufProviderHelper.class);

  private final BuilderFactory builderFactory = new BuilderFactory();

  private final ExtensionRegistryFactory extensionRegistryFactory = new ExtensionRegistryFactory();

  public ProtobufProviderHelper() {
    log.info("patate!");
  }

  public BuilderFactory builders() {
    return builderFactory;
  }

  public ExtensionRegistryFactory extensions() {
    return extensionRegistryFactory;
  }

  protected static final class ExtensionRegistryFactory {

    private Map<Class<?>, ExtensionRegistry> registryCache = new HashMap<Class<?>, ExtensionRegistry>();

    private Map<Class<?>, Method> methodCache = new HashMap<Class<?>, Method>();

    ExtensionRegistry forMessage(final Class<Message> messageType) {
      if(messageType == null) throw new IllegalArgumentException("messageType cannot be null");

      Class<?> enclosingType = messageType.getEnclosingClass();
      if(registryCache.containsKey(enclosingType) == false) {
        ExtensionRegistry registry = ExtensionRegistry.newInstance();
        invokeStaticMethod(extractStaticMethod("registerAllExtensions", methodCache, messageType.getEnclosingClass(), ExtensionRegistry.class), registry);
        registryCache.put(enclosingType, registry);
      }
      return registryCache.get(enclosingType);
    }
  }

  protected static final class BuilderFactory {

    private Map<Class<?>, Method> methodCache = new HashMap<Class<?>, Method>();

    Builder forMessage(final Class<Message> messageType) {
      if(messageType == null) throw new IllegalArgumentException("messageType cannot be null");
      return (Builder) invokeStaticMethod(extractStaticMethod("newBuilder", methodCache, messageType));
    }

  }

  private static Object invokeStaticMethod(Method method, Object... arguments) {
    if(method == null) throw new IllegalArgumentException("method cannot be null");

    try {
      return method.invoke(null, arguments);
    } catch(WebApplicationException e) {
      throw e;
    } catch(RuntimeException e) {
      log.error("Error invoking '" + method.getName() + "' method for type " + method.getDeclaringClass().getName(), e);
      throw new WebApplicationException(500);
    } catch(IllegalAccessException e) {
      log.error("Error invoking '" + method.getName() + "' method for type " + method.getDeclaringClass().getName(), e);
      throw new WebApplicationException(500);
    } catch(InvocationTargetException e) {
      log.error("Error invoking '" + method.getName() + "' method for type " + method.getDeclaringClass().getName(), e);
      throw new WebApplicationException(500);
    }
  }

  private static Method extractStaticMethod(final String methodName, final Map<Class<?>, Method> methodCache, final Class<?> type, Class<?>... arguments) {
    if(methodName == null) throw new IllegalArgumentException("methodName cannot be null");
    if(methodCache == null) throw new IllegalArgumentException("methodCache cannot be null");
    if(type == null) throw new IllegalArgumentException("type cannot be null");

    if(methodCache.containsKey(type) == false) {
      try {
        methodCache.put(type, type.getMethod(methodName, arguments));
      } catch(SecurityException e) {
        log.error("Error getting '" + methodName + "' method from type " + type.getName(), e);
        throw new WebApplicationException(500);
      } catch(NoSuchMethodException e) {
        throw new IllegalStateException("The type " + type.getName() + " does not define a '" + methodName + "' static method.");
      }
    }
    return methodCache.get(type);
  }
}
