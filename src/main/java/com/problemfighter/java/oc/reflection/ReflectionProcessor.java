package com.problemfighter.java.oc.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class ReflectionProcessor {

    public List<Class<?>> getAllSuperClass(Class<?> klass) {
        List<Class<?>> classes = new ArrayList<>();
        if (klass != null) {
            Class<?> superclass = klass.getSuperclass();
            while (superclass != null) {
                classes.add(superclass);
                superclass = superclass.getSuperclass();
            }
        }
        return classes;
    }

    public List<Class<?>> getAllClass(Class<?> klass) {
        if (klass == null) {
            return new ArrayList<>();
        }
        List<Class<?>> classes = getAllSuperClass(klass);
        classes.add(klass);
        return classes;
    }

    public List<Field> getAllField(Class<?> klass) {
        List<Field> fields = new ArrayList<>();
        if (klass != null) {
            List<Class<?>> classes = getAllClass(klass);
            for (Class<?> pClass : classes) {
                fields.addAll(Arrays.asList(pClass.getDeclaredFields()));
            }
        }
        return fields;
    }

    private Field getDeclaredField(Object object, String fieldName) {
        return getDeclaredField(object.getClass(), fieldName);
    }

    private Field getDeclaredField(Class<?> klass, String fieldName) {
        try {
            return klass.getDeclaredField(fieldName);
        } catch (NoSuchFieldException ignore) {
        }
        return null;
    }

    private Field getField(Object object, String fieldName) {
        try {
            return object.getClass().getField(fieldName);
        } catch (NoSuchFieldException ignore) {
        }
        return null;
    }

    public Field getFieldFromObject(Object object, String fieldName) {
        return getFieldFromObject(object.getClass(), fieldName);
    }

    public Field getFieldFromObject(Class<?> klass, String fieldName) {
        Field field = getDeclaredField(klass, fieldName);
        if (field == null) {
            field = getField(klass, fieldName);
        }
        if (field != null) {
            field.setAccessible(true);
        }
        return field;
    }

    public Field getAnyFieldFromObject(Object object, String fieldName) {
        return getAnyFieldFromKlass(object.getClass(), fieldName);
    }

    public Field getAnyFieldFromKlass(Class<?> klass, String fieldName) {
        Field field = getFieldFromObject(klass, fieldName);
        if (field == null) {
            Class<?> superclass = klass.getSuperclass();
            while (superclass != null) {
                field = getDeclaredField(superclass, fieldName);
                if (field != null) {
                    return field;
                }
                superclass = superclass.getSuperclass();
            }
        }
        return field;
    }


    public <D> D newInstance(Class<D> klass) {
        try {
            return klass.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignore) {
        }
        return null;
    }

    public Boolean isPrimitive(Class<?> c) {
        return c.isPrimitive() || c == String.class || c == Boolean.class || c == Byte.class || c == Short.class || c == Character.class
                || c == Integer.class || c == Float.class || c == Double.class || c == BigDecimal.class || c == BigInteger.class
                || c == LocalDate.class || c == LocalDateTime.class || c == Date.class || c == Timestamp.class || c == Long.class;
    }


    public Boolean isList(Class<?> c) {
        return c == List.class || c == Collection.class || c == LinkedList.class || c == ArrayList.class || c == Vector.class || c == Stack.class;
    }

    public Boolean isSet(Class<?> c) {
        return c == TreeSet.class || c == Set.class || c == LinkedHashSet.class || c == HashSet.class || c == SortedSet.class;
    }

    public Boolean isQueue(Class<?> c) {
        return c == Queue.class || c == PriorityQueue.class || c == Deque.class;
    }

    public Boolean isMap(Class<?> c) {
        return c == Map.class || c == LinkedHashMap.class || c == HashMap.class || c == SortedMap.class || c == TreeMap.class;
    }

    public Collection<?> instanceOfList(Class<?> c) {
        if (c == LinkedList.class) {
            return new LinkedList<>();
        } else if (c == Vector.class) {
            return new Vector<>();
        } else if (c == Stack.class) {
            return new Stack<>();
        }
        return new ArrayList<>();
    }

    public Queue<?> instanceOfQueue(Class<?> c) {
        if (c == ArrayDeque.class || c == Deque.class) {
            return new ArrayDeque<>();
        }
        return new PriorityQueue<>();
    }

    public Set<?> instanceOfSet(Class<?> c) {
        if (c == TreeSet.class || c == SortedSet.class) {
            return new TreeSet<>();
        } else if (c == HashSet.class) {
            return new HashSet<>();
        }
        return new LinkedHashSet<>();
    }

    public Map<?, ?> instanceOfMap(Class<?> c) {
        if (c == HashMap.class) {
            return new HashMap<>();
        } else if (c == TreeMap.class || c == SortedMap.class) {
            return new TreeMap<>();
        }
        return new LinkedHashMap<>();
    }

    public Method getMethod(Class<?> c, String name, Class<?>... parameterTypes) throws NoSuchMethodException {
      return c.getDeclaredMethod(name, parameterTypes);
    }

    public Boolean isMethodExist(Class<?> c, String name, Class<?>... parameterTypes) {
        try {
            getMethod(c, name, parameterTypes);
            return true;
        } catch (NoSuchMethodException ignore) {
        }
        return false;
    }

    public Object invokeMethod(Object object, String name, Object... parameterTypes) {
        try {
            int paramLength = parameterTypes.length;
            Class<?>[] classes = new Class[paramLength];
            for (int i = 0; i < paramLength; i++) {
                classes[i] = parameterTypes[i].getClass();
            }
            Method method = getMethod(object.getClass(), name, classes);
            if (method != null) {
                return method.invoke(object, parameterTypes);
            }
        } catch (NoSuchMethodException | IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            } else if (e.getCause() instanceof Error) {
                throw (Error) e.getCause();
            }
        }
        return null;
    }

}
