package cn.bravedawn.serviceImpl;

import cn.bravedawn.bean.RecordChannel;
import cn.bravedawn.bean.UserRecord;
import cn.bravedawn.common.JsonBean;
import cn.bravedawn.common.JsonBeanBuilder;
import cn.bravedawn.common.ResponseCode;
import cn.bravedawn.common.UrlType;
import cn.bravedawn.repository.RecordChannelRepository;
import cn.bravedawn.repository.UserChannelRepository;
import cn.bravedawn.repository.UserRecordRepository;
import cn.bravedawn.service.UserChannelService;
import cn.bravedawn.service.UserRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by 冯晓 on 2017/10/9.
 */

@Service
public class UserRecordServiceImpl implements UserRecordService{

    @Autowired
    private UserRecordRepository userRecordRepository;

    @Autowired
    private RecordChannelRepository recordChannelRepository;

    @Autowired
    private UserChannelRepository userChannelRepository;

    @Transactional
    public void update(Integer id, Integer userId){
        userRecordRepository.update(id, userId);
    }

    public JsonBean queryAllByPage(Integer userId, Pageable pageable){
        //JsonBean jsonBean = new JsonBean();
        Specification<UserRecord> specification = new Specification<UserRecord>() {
            @Override
            public Predicate toPredicate(Root<UserRecord> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
                Path path = root.get("userId");
                return criteriaBuilder.equal(path, userId);
            }
        };
        Page<UserRecord> userRecordPage = userRecordRepository.findAll(specification, pageable);

        for (UserRecord userRecord : userRecordPage.getContent()){
            RecordChannel recordChannel = recordChannelRepository.findByRecordId(userRecord.getId());
            if (recordChannel != null){
                userRecord.setChannel(userChannelRepository.findOne(recordChannel.getChannelId()).getChannel());
            } else{
                userRecord.setChannel("");
            }
            if (userRecord.getStar().equals("a")){
                userRecord.setMStar(false);
            } else{
                userRecord.setMStar(true);
            }
        }
        return JsonBeanBuilder.builder()
                .setCode(ResponseCode.SUCCESS.getCode())
                .setMsg(ResponseCode.SUCCESS.getDesc())
                .setTotalPage(userRecordPage.getTotalPages())
                .setTotal((int) userRecordPage.getTotalElements())
                .setPage(userRecordPage.getNumber())
                .setData(userRecordPage.getContent())
                .setPageSize(userRecordPage.getNumberOfElements())
                .build();
    }


    public JsonBean add(UserRecord userRecord){
        userRecord.setResource(getResource(userRecord.getUrl()));
        userRecord.setUpdateDate(new Date());
        userRecord.setStar("a");
        userRecordRepository.save(userRecord);
        if (userRecord.getStar().equals("a")){
            userRecord.setMStar(false);
        } else{
            userRecord.setMStar(true);
        }
        List<UserRecord> records = new ArrayList<>();
        records.add(userRecord);
        return JsonBeanBuilder.builder()
                .setCode(ResponseCode.SUCCESS.getCode())
                .setMsg(ResponseCode.SUCCESS.getDesc())
                .setData(records)
                .build();
    }

    public JsonBean delete(Integer recordId){
        UserRecord record = userRecordRepository.findOne(recordId);
        if (record == null){
            return JsonBeanBuilder.builder()
                    .setCode(ResponseCode.ILLEGAL_ARGUMENT.getCode())
                    .setMsg("该记录不存在")
                    .build();
        }
        userRecordRepository.delete(recordId);
        return JsonBeanBuilder.builder()
                .setCode(ResponseCode.SUCCESS.getCode())
                .setMsg(ResponseCode.SUCCESS.getDesc())
                .build();

    }

    public JsonBean addStar(Integer recordId){
        UserRecord record = userRecordRepository.findOne(recordId);
        if (record == null){
            return JsonBeanBuilder.builder()
                    .setCode(ResponseCode.ILLEGAL_ARGUMENT.getCode())
                    .setMsg("该记录不存在")
                    .build();
        }
        record.setStar("b");
        userRecordRepository.save(record);
        return JsonBeanBuilder.builder()
                .setCode(ResponseCode.SUCCESS.getCode())
                .setMsg(ResponseCode.SUCCESS.getDesc())
                .build();

    }

    public JsonBean deleteStar(Integer recordId){
        UserRecord record = userRecordRepository.findOne(recordId);
        if (record == null){
            return JsonBeanBuilder.builder()
                    .setCode(ResponseCode.ILLEGAL_ARGUMENT.getCode())
                    .setMsg("该记录不存在")
                    .build();
        }
        record.setStar("a");
        userRecordRepository.save(record);
        return JsonBeanBuilder.builder()
                .setCode(ResponseCode.SUCCESS.getCode())
                .setMsg(ResponseCode.SUCCESS.getDesc())
                .build();

    }

    public JsonBean getStar(Integer userId){
        List<UserRecord> userRecordList = userRecordRepository.findAllByUserIdAndStar(userId, "b");
        for (UserRecord record : userRecordList){
            RecordChannel recordChannel = recordChannelRepository.findByRecordId(record.getId());
            if (recordChannel != null){
                record.setChannel(userChannelRepository.findOne(recordChannel.getChannelId()).getChannel());
            } else{
                record.setChannel("");
            }
            record.setMStar(true);
        }
        return JsonBeanBuilder.builder()
                .setCode(ResponseCode.SUCCESS.getCode())
                .setMsg(ResponseCode.SUCCESS.getDesc())
                .setTotal(userRecordList.size())
                .setData(userRecordList)
                .build();
    }


    private String getResource(String url){
        if (url.contains(UrlType.WEIXIN.getKeyWord())){
            return UrlType.WEIXIN.getUrl();
        } else if (url.contains(UrlType.JIANSHU.getKeyWord())){
            return UrlType.JIANSHU.getUrl();
        } else if (url.contains(UrlType.ZHIHU.getKeyWord())){

            return UrlType.ZHIHU.getUrl();
        } else if (url.contains(UrlType.JUEJIN.getKeyWord())){

            return UrlType.JUEJIN.getUrl();
        } else if (url.contains(UrlType.WEIBO.getKeyWord())){

            return UrlType.WEIBO.getUrl();
        } else{
            return url;
        }

    }

    public JsonBean searchByKeyword(Integer userId, String keyword){
        List<UserRecord> userRecordList = userRecordRepository.findByUserIdAndTitleLike(userId, "%"+keyword+"%");
        if (userRecordList.size() == 0){
            return JsonBeanBuilder.builder()
                    .setCode(ResponseCode.ERROR.getCode())
                    .setMsg("没有搜索到相关结果")
                    .setTotal(0)
                    .build();
        }
        return JsonBeanBuilder.builder()
                .setCode(ResponseCode.SUCCESS.getCode())
                .setMsg(ResponseCode.SUCCESS.getDesc())
                .setData(userRecordList)
                .setTotal(userRecordList.size())
                .build();
    }
}
