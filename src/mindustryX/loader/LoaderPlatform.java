package mindustryX.loader;

import java.net.*;
import java.util.*;

interface LoaderPlatform{
    /** Run method in a normal classloader(not ModClassloader), including mod and parent. */
    void withSafeClassloader(String method);

    ClassLoader createClassloader();

    default void beforeLaunch(){
    }

    void launch(ClassLoader loader) throws Exception;

    final class CompoundURLEnumeration implements Enumeration<URL>{
        private final Enumeration<URL>[] enums;
        private int index;

        @SafeVarargs
        public CompoundURLEnumeration(Enumeration<URL>... enums){
            this.enums = enums;
        }

        private boolean next(){
            while(index < enums.length){
                if(enums[index] != null && enums[index].hasMoreElements()){
                    return true;
                }
                index++;
            }
            return false;
        }

        public boolean hasMoreElements(){
            return next();
        }

        public URL nextElement(){
            if(!next()){
                throw new NoSuchElementException();
            }
            return enums[index].nextElement();
        }
    }
}
