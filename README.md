# ck-mvc
canal-kafka mvc，consumer can be restful

## summary
- we can use mapping fit different db event depend on restful action like spring mvc
- we can directly get business dto by raw msg(must be first param)
- we support most parts of features in kafkaListener


## fist of all
- add annotation @EnableCanalController in your startApplication 
- add annotation @CanalController in your consumer class ,at least put topicName
- add mapping annotation in your method ,at least `value` or `path` means tableName,`name` means consumerId(you can also use class consumerId in CanalController)
- have a cup of coffee and enjoy your business code

### code example
```java

@CanalController(topics = {"${spring.kafka.consumerGroups.couponCanalGroup.topics:coupon}"})
public class TestConsumer {

    @PostMapping(value = "tableName", name = "consumerGroupId1")
    public void onMessage(ConsumerRecord<String, String> data, Acknowledgment acknowledgment) {
         System.out.println("insert operation");
        acknowledgment.acknowledge();
    }

    @RequestMapping(value = "tableName", method = RequestMethod.PUT, name = "consumerGroupId2")
    public void onMessage2(ConsumerRecord<String, String> data, Acknowledgment acknowledgment) {
        System.out.println("update operation");
        acknowledgment.acknowledge();
    }

    @DeleteMapping(path = "tableName", name = "consumerGroupId3")
    public void onMessage3(ConsumerRecord<String, String> data, Acknowledgment acknowledgment) {
        System.out.println("delete operation");
        acknowledgment.acknowledge();
    }

    @DeleteMapping(path = "tableName", name = "consumerGroupId4")
    public void onMessage4(List<BusinessDTO> businessDTOList, Acknowledgment acknowledgment) {
        System.out.println("delete operation");
        acknowledgment.acknowledge();
    }

    @RequestMapping(value = "tableName",  name = "consumerGroupId5")
    public void onMessage2(CanalTypeEnum handleType, @RequestParam(CanalDataType.BEFORE) List<BusinessDTO> businessDTOList,
        @RequestParam(CanalDataType.AFTER) List<BusinessDTO> businessDTOList, Acknowledgment acknowledgment) {
        System.out.println("update operation");
        acknowledgment.acknowledge();
    }


    private class BusinessDTO {
        private String id;

        private String name;

        public void setId(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
```

## detail
| mapping | Action |
| ------- | ------ |
| post    | insert |
| put     | Update |
| delete  | delete |



