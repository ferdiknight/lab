package com.taobao.common.store.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.log4j.Logger;

/**
 * Ϊ�˲������������İ�,������д��
 * @author shuihan
 *
 */
public class SerializerUtil {
        private static final Logger logger = Logger.getLogger(SerializerUtil.class);

        /**
         * java�����л�
         * @param objContent
         * @return
         * @throws IOException
         */
        public static Object decodeObject(byte[] objContent) throws IOException{
            Object obj = null;
            ByteArrayInputStream bais = null;
            ObjectInputStream ois = null;
            try {
                bais = new ByteArrayInputStream(objContent);
                ois = new ObjectInputStream(bais);
                obj = ois.readObject();
            } catch (IOException ex) {
                throw ex;
            } catch (ClassNotFoundException ex) {
                logger.warn("Failed to decode object.", ex);
            } finally {
                if (ois != null) {
                    try {
                        ois.close();
                        bais.close();
                    } catch (IOException ex) {
                        logger.error("Failed to close stream.", ex);
                    }
                }
            }
            return obj;
        }


        /**
         * java���л�
         * @param objContent
         * @return
         * @throws IOException
         */
        public static byte[] encodeObject(Object objContent) throws IOException {
            ByteArrayOutputStream baos = null;
            ObjectOutputStream output = null;
            try {
                baos = new ByteArrayOutputStream(1024);
                output = new ObjectOutputStream(baos);
                output.writeObject(objContent);
            }
            catch (IOException ex) {
                throw ex;

            }
            finally {
                if (output != null) {
                    try {
                        output.close();
                        if (baos != null) {
                            baos.close();
                        }
                    }
                    catch (IOException ex) {
                       logger.error("Failed to close stream.", ex);
                    }
                }
            }
            return baos != null ? baos.toByteArray() : null;
        }

}
