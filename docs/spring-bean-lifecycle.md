# Spring Bean生命周期详解

> Spring Bean从创建到销毁的完整生命周期，以及可以介入的扩展点

---

## 一、Bean生命周期流程图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          Spring Bean 生命周期                                │
└─────────────────────────────────────────────────────────────────────────────┘

1. 实例化（Instantiation）
   │
   ├── 调用构造方法创建对象
   │
   ▼
2. 属性赋值（Populate Properties）
   │
   ├── 依赖注入（DI）
   ├── @Autowired、@Value等注解处理
   │
   ▼
3. 初始化（Initialization）
   │
   ├── 3.1 设置Aware接口（获取Spring容器资源）
   │     ├── BeanNameAware.setBeanName()
   │     ├── BeanClassLoaderAware.setBeanClassLoader()
   │     ├── BeanFactoryAware.setBeanFactory()
   │     ├── EnvironmentAware.setEnvironment()
   │     ├── ApplicationContextAware.setApplicationContext()
   │     └── ...
   │
   ├── 3.2 BeanPostProcessor.postProcessBeforeInitialization()
   │     └── @PostConstruct注解处理
   │
   ├── 3.3 InitializingBean.afterPropertiesSet()
   │
   ├── 3.4 自定义init-method
   │
   └── 3.5 BeanPostProcessor.postProcessAfterInitialization()
   │       └── AOP代理创建（如果有切面）
   │
   ▼
4. Bean就绪，可以使用
   │
   ├── 业务逻辑执行
   │
   ▼
5. 销毁（Destruction）
   │
   ├── 5.1 @PreDestroy注解处理
   │
   ├── 5.2 DisposableBean.destroy()
   │
   └── 5.3 自定义destroy-method
   │
   ▼
6. Bean销毁完成
```

---

## 二、四个阶段详解

### 阶段1：实例化（Instantiation）

```java
// Spring通过反射调用构造方法创建对象
ApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
UserService userService = context.getBean(UserService.class);

// 1. 推断构造方法（如果有多个，选择@Autowired标注的或默认无参）
// 2. 通过反射调用构造方法
// 3. 创建原始对象（还未注入依赖）
```

**扩展点：InstantiationAwareBeanPostProcessor**
```java
@Component
public class MyInstantiationAwareBeanPostProcessor implements InstantiationAwareBeanPostProcessor {
    
    // 在实例化之前调用，可以返回代理对象替代正常实例化
    @Override
    public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) {
        System.out.println("实例化前: " + beanName);
        return null; // 返回null表示正常实例化
    }
    
    // 在属性赋值前调用
    @Override
    public boolean postProcessAfterInstantiation(Object bean, String beanName) {
        System.out.println("实例化后: " + beanName);
        return true; // 返回true表示继续属性赋值
    }
}
```

---

### 阶段2：属性赋值（Populate Properties）

```java
@Component
public class UserService {
    
    @Autowired  // 在这里进行依赖注入
    private UserDao userDao;
    
    @Value("${app.name}")  // 注入配置
    private String appName;
}
```

**扩展点：AutowiredAnnotationBeanPostProcessor**
```java
// 处理@Autowired、@Value、@Inject注解
// 通过反射或setter方法注入依赖
```

---

### 阶段3：初始化（Initialization）

这是扩展点最多的阶段，按执行顺序：

#### 3.1 Aware接口（获取容器资源）

```java
@Component
public class MyBean implements 
    BeanNameAware,           // 获取beanName
    BeanFactoryAware,        // 获取BeanFactory
    ApplicationContextAware, // 获取ApplicationContext
    EnvironmentAware {       // 获取Environment
    
    private String beanName;
    private ApplicationContext context;
    
    @Override
    public void setBeanName(String name) {
        this.beanName = name;
        System.out.println("BeanNameAware: " + name);
    }
    
    @Override
    public void setApplicationContext(ApplicationContext ctx) {
        this.context = ctx;
        System.out.println("ApplicationContextAware");
    }
    
    // ... 其他Aware方法
}
```

**Aware接口执行顺序**：
```
1. BeanNameAware.setBeanName()
2. BeanClassLoaderAware.setBeanClassLoader()
3. BeanFactoryAware.setBeanFactory()
4. EnvironmentAware.setEnvironment()
5. EmbeddedValueResolverAware.setEmbeddedValueResolver()
6. ResourceLoaderAware.setResourceLoader()
7. ApplicationEventPublisherAware.setApplicationEventPublisher()
8. MessageSourceAware.setMessageSource()
9. ApplicationContextAware.setApplicationContext()
10. ServletContextAware.setServletContext()（Web环境）
```

#### 3.2 BeanPostProcessor.postProcessBeforeInitialization()

```java
@Component
public class MyBeanPostProcessor implements BeanPostProcessor {
    
    // 初始化前处理
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        System.out.println("初始化前: " + beanName);
        // 可以修改bean或返回代理对象
        return bean;
    }
}

// 处理@PostConstruct注解的是InitDestroyAnnotationBeanPostProcessor
```

#### 3.3 @PostConstruct注解

```java
@Component
public class MyService {
    
    @PostConstruct  // 在InitializingBean之前执行
    public void init() {
        System.out.println("@PostConstruct执行");
        // 初始化逻辑：如缓存预热、连接检查等
    }
}
```

#### 3.4 InitializingBean接口

```java
@Component
public class MyService implements InitializingBean {
    
    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("InitializingBean.afterPropertiesSet()");
        // 属性设置后的初始化逻辑
    }
}
```

#### 3.5 自定义init-method

```java
@Component
public class MyService {
    
    public void customInit() {
        System.out.println("自定义init-method");
    }
}

// XML配置
<bean id="myService" class="com.example.MyService" init-method="customInit"/>

// 或注解配置
@Bean(initMethod = "customInit")
public MyService myService() {
    return new MyService();
}
```

#### 3.6 BeanPostProcessor.postProcessAfterInitialization()

```java
@Component
public class MyBeanPostProcessor implements BeanPostProcessor {
    
    // 初始化后处理 - AOP代理在此创建
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        System.out.println("初始化后: " + beanName);
        
        // 如果有切面，返回代理对象
        if (needProxy(bean)) {
            return createProxy(bean);
        }
        return bean;
    }
}
```

**初始化阶段执行顺序总结**：
```
1. Aware接口方法
2. BeanPostProcessor.postProcessBeforeInitialization()
3. @PostConstruct
4. InitializingBean.afterPropertiesSet()
5. 自定义init-method
6. BeanPostProcessor.postProcessAfterInitialization() ← AOP代理创建
```

---

### 阶段4：销毁（Destruction）

```java
@Component
public class MyService implements DisposableBean {
    
    @PreDestroy  // 1. 最先执行
    public void preDestroy() {
        System.out.println("@PreDestroy执行");
        // 释放资源前的预处理
    }
    
    @Override  // 2. 其次执行
    public void destroy() throws Exception {
        System.out.println("DisposableBean.destroy()");
        // 释放资源
    }
    
    // 3. 最后执行 - 自定义destroy-method
    public void customDestroy() {
        System.out.println("自定义destroy-method");
    }
}

// 触发销毁
context.close();  // 或 context.registerShutdownHook()
```

**销毁阶段执行顺序**：
```
1. @PreDestroy
2. DisposableBean.destroy()
3. 自定义destroy-method
```

---

## 三、扩展点总结

| 扩展点 | 接口/注解 | 执行时机 | 用途 |
|--------|----------|---------|------|
| **实例化前** | InstantiationAwareBeanPostProcessor.postProcessBeforeInstantiation() | 实例化之前 | 返回代理对象替代正常实例化 |
| **实例化后** | InstantiationAwareBeanPostProcessor.postProcessAfterInstantiation() | 实例化之后，属性赋值之前 | 控制是否继续属性赋值 |
| **属性处理** | InstantiationAwareBeanPostProcessor.postProcessProperties() | 属性赋值时 | 自定义属性注入逻辑 |
| **Aware接口** | XxxAware | 初始化之前 | 获取Spring容器资源 |
| **初始化前** | BeanPostProcessor.postProcessBeforeInitialization() | 初始化之前 | 修改bean，处理@PostConstruct |
| **初始化** | @PostConstruct | 属性赋值后 | 初始化逻辑 |
| **初始化** | InitializingBean | @PostConstruct后 | 初始化逻辑 |
| **初始化** | init-method | InitializingBean后 | 自定义初始化 |
| **初始化后** | BeanPostProcessor.postProcessAfterInitialization() | 初始化之后 | AOP代理创建，修改bean |
| **销毁前** | @PreDestroy | 销毁之前 | 资源释放前处理 |
| **销毁** | DisposableBean | @PreDestroy后 | 资源释放 |
| **销毁** | destroy-method | DisposableBean后 | 自定义销毁 |

---

## 四、实战案例：Bean生命周期监控

```java
@Component
public class BeanLifecycleDemo implements 
    BeanNameAware, 
    ApplicationContextAware, 
    InitializingBean, 
    DisposableBean {
    
    @Autowired
    private UserDao userDao;
    
    public BeanLifecycleDemo() {
        System.out.println("1. 构造方法执行");
    }
    
    @Override
    public void setBeanName(String name) {
        System.out.println("2. BeanNameAware.setBeanName(): " + name);
    }
    
    @Override
    public void setApplicationContext(ApplicationContext ctx) {
        System.out.println("3. ApplicationContextAware.setApplicationContext()");
    }
    
    @PostConstruct
    public void postConstruct() {
        System.out.println("4. @PostConstruct执行");
    }
    
    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("5. InitializingBean.afterPropertiesSet()");
    }
    
    public void customInit() {
        System.out.println("6. 自定义init-method");
    }
    
    // Bean就绪，执行业务逻辑...
    
    @PreDestroy
    public void preDestroy() {
        System.out.println("7. @PreDestroy执行");
    }
    
    @Override
    public void destroy() throws Exception {
        System.out.println("8. DisposableBean.destroy()");
    }
    
    public void customDestroy() {
        System.out.println("9. 自定义destroy-method");
    }
}

// 输出顺序：
// 1. 构造方法执行
// 2. BeanNameAware.setBeanName()
// 3. ApplicationContextAware.setApplicationContext()
// 4. @PostConstruct执行
// 5. InitializingBean.afterPropertiesSet()
// 6. 自定义init-method
// ... Bean使用 ...
// 7. @PreDestroy执行
// 8. DisposableBean.destroy()
// 9. 自定义destroy-method
```

---

## 五、面试话术（完整版）

### 简短版（1分钟）

```
Spring Bean生命周期分为四个阶段：

1. 实例化：调用构造方法创建对象
2. 属性赋值：依赖注入
3. 初始化：执行Aware接口、@PostConstruct、InitializingBean、init-method
4. 销毁：执行@PreDestroy、DisposableBean、destroy-method

主要扩展点：
- BeanPostProcessor：初始化前后处理
- Aware接口：获取容器资源
- @PostConstruct/@PreDestroy：初始化和销毁
```

### 详细版（3-5分钟）

```
【Bean生命周期】

Spring Bean生命周期分为四个阶段：

1. 实例化（Instantiation）
   通过反射调用构造方法创建对象

2. 属性赋值（Populate Properties）
   进行依赖注入，处理@Autowired、@Value等注解

3. 初始化（Initialization）
   按顺序执行：
   - Aware接口（获取BeanName、ApplicationContext等）
   - BeanPostProcessor.postProcessBeforeInitialization()
   - @PostConstruct
   - InitializingBean.afterPropertiesSet()
   - 自定义init-method
   - BeanPostProcessor.postProcessAfterInitialization()（AOP代理创建）

4. 销毁（Destruction）
   按顺序执行：
   - @PreDestroy
   - DisposableBean.destroy()
   - 自定义destroy-method

【扩展点】

1. BeanPostProcessor：
   最常用的扩展点，在初始化前后处理bean
   postProcessBeforeInitialization和postProcessAfterInitialization

2. Aware接口：
   让Bean获取Spring容器资源
   常用：ApplicationContextAware、BeanNameAware

3. @PostConstruct/@PreDestroy：
   JSR-250标准注解，用于初始化和销毁

4. InitializingBean/DisposableBean：
   Spring提供的接口，功能与注解类似

【AOP代理创建时机】

AOP代理在BeanPostProcessor.postProcessAfterInitialization()中创建，
所以@PostConstruct和InitializingBean中调用的方法是原始对象的方法，
不会被AOP拦截。
```

---

## 六、高频面试题

### Q1：Bean的生命周期？

```
四个阶段：实例化 → 属性赋值 → 初始化 → 销毁

初始化阶段：Aware → @PostConstruct → InitializingBean → init-method
销毁阶段：@PreDestroy → DisposableBean → destroy-method
```

### Q2：有哪些扩展点可以介入？

```
1. InstantiationAwareBeanPostProcessor：实例化前后
2. BeanPostProcessor：初始化前后（最常用）
3. Aware接口：获取容器资源
4. @PostConstruct/@PreDestroy：初始化和销毁
5. InitializingBean/DisposableBean：初始化和销毁
6. init-method/destroy-method：自定义初始化和销毁
```

### Q3：@PostConstruct和InitializingBean的区别？

```
执行顺序：@PostConstruct先，InitializingBean后

@PostConstruct：
- JSR-250标准注解
- 与Spring解耦
- 推荐优先使用

InitializingBean：
- Spring专用接口
- 与Spring耦合
- 代码侵入性强
```

### Q4：AOP代理什么时候创建？

```
在BeanPostProcessor.postProcessAfterInitialization()中创建

所以：
- @PostConstruct中调用的是原始对象的方法
- 不会被AOP拦截
- 如果需要AOP拦截，要用其他方式
```

### Q5：BeanPostProcessor和Aware接口的执行顺序？

```
Aware接口先执行，然后才是BeanPostProcessor

具体：
1. Aware接口方法
2. BeanPostProcessor.postProcessBeforeInitialization()
3. 初始化方法
4. BeanPostProcessor.postProcessAfterInitialization()
```

---

## 七、记忆口诀

```
Bean生命周期四步走，
实例属性初始化销毁。

Aware接口先执行，
获取资源很方便。

PostProcessor在中间，
前后处理都包揽。

PostConstruct初始化，
PreDestroy销毁时。

InitializingBean也可以，
注解方式更推荐。

AOP代理最后建，
生命周期记心间。
```

---

**掌握标准**：能说清四个阶段、初始化顺序、主要扩展点、执行时机。
