package com.zyy.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zyy.inventory.mapper.ConsumableMapper;
import com.zyy.inventory.mapper.InventoryTransactionMapper;
import com.zyy.inventory.model.dto.ConsumableSaveDTO;
import com.zyy.inventory.model.entity.ConsumableEntity;
import com.zyy.inventory.model.vo.ConsumableVO;
import com.zyy.common.PageVO;
import com.zyy.inventory.service.impl.ConsumableServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ConsumableService 业务逻辑单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConsumableService 业务逻辑测试")
class ConsumableServiceTest {

    @Mock
    private ConsumableMapper consumableMapper;

    @Mock
    private InventoryTransactionMapper transactionMapper;

    @Mock
    private com.zyy.inventory.service.StockAlertService stockAlertService;

    @InjectMocks
    private ConsumableServiceImpl consumableService;

    private ConsumableEntity testEntity;
    private ConsumableSaveDTO testSaveDTO;

    @BeforeEach
    void setUp() {
        testEntity = new ConsumableEntity();
        testEntity.setId(1L);
        testEntity.setName("测试耗材");
        testEntity.setProductCode("CODE001");
        testEntity.setCategory("类别A");
        testEntity.setUnit("个");
        testEntity.setStockQuantity(100);
        testEntity.setMinStockLevel(10);
        testEntity.setMaxStockLevel(200);
        testEntity.setUnitCost(new BigDecimal("9.99"));
        testEntity.setExpirationDate(LocalDate.now().plusDays(30));
        testEntity.setSupplier("测试供应商");
        testEntity.setStorageLocation("A-01-01");
        testEntity.setStatus(1);
        testEntity.setIsDeleted(0);
        testEntity.setCreateUser(1L);

        testSaveDTO = new ConsumableSaveDTO();
        testSaveDTO.setProductCode("CODE001");
        testSaveDTO.setName("测试耗材");
        testSaveDTO.setCategory("类别A");
        testSaveDTO.setUnit("个");
        testSaveDTO.setStockQuantity(100);
        testSaveDTO.setMinStockLevel(10);
        testSaveDTO.setMaxStockLevel(200);
        testSaveDTO.setUnitCost(new BigDecimal("9.99"));
        testSaveDTO.setExpirationDate(LocalDate.now().plusDays(30));
    }

    @Test
    @DisplayName("新增耗材成功")
    void saveSuccess() {
        when(consumableMapper.selectCount(any())).thenReturn(0L);
        when(consumableMapper.insert(any())).thenReturn(1);

        ConsumableVO result = consumableService.save(testSaveDTO, 1L);

        assertNotNull(result);
        verify(consumableMapper).insert(any());
    }

    @Test
    @DisplayName("新增耗材时产品编码已存在则抛异常")
    void saveDuplicateProductCode() {
        when(consumableMapper.selectCount(any())).thenReturn(1L);

        assertThrows(Exception.class, () -> consumableService.save(testSaveDTO, 1L));
    }

    @Test
    @DisplayName("分页查询耗材列表")
    void getPageSuccess() {
        Page<ConsumableEntity> mockPage = new Page<>(1, 10);
        mockPage.setRecords(List.of(testEntity));
        mockPage.setTotal(1L);
        when(consumableMapper.selectPage(any(Page.class), any())).thenReturn(mockPage);

        PageVO<ConsumableVO> result = consumableService.getPage(1L, 10L, null, null, null);

        assertNotNull(result);
        assertEquals(1, result.getItems().size());
        assertEquals(1L, result.getTotal());
    }

    @Test
    @DisplayName("根据ID获取耗材成功")
    void getByIdSuccess() {
        when(consumableMapper.selectById(1L)).thenReturn(testEntity);

        ConsumableVO result = consumableService.getById(1L);

        assertNotNull(result);
        assertEquals("测试耗材", result.getName());
        assertEquals("CODE001", result.getProductCode());
    }

    @Test
    @DisplayName("根据ID查询不存在返回null")
    void getByIdNotFound() {
        when(consumableMapper.selectById(999L)).thenReturn(null);

        ConsumableVO result = consumableService.getById(999L);

        assertNull(result);
    }

    @Test
    @DisplayName("出库操作减少库存")
    void outboundReducesStock() {
        when(consumableMapper.selectById(1L)).thenReturn(testEntity);
        when(consumableMapper.updateById(any())).thenReturn(1);
        when(transactionMapper.insert(any())).thenReturn(1);

        consumableService.outbound(1L, 10, "REF001", "出库测试", 1L);

        ArgumentCaptor<ConsumableEntity> captor = ArgumentCaptor.forClass(ConsumableEntity.class);
        verify(consumableMapper).updateById(captor.capture());
        assertEquals(90, captor.getValue().getStockQuantity()); // 100 - 10
    }

    @Test
    @DisplayName("出库数量超过库存时抛出异常")
    void outboundOverStockThrows() {
        when(consumableMapper.selectById(1L)).thenReturn(testEntity);

        assertThrows(Exception.class, () -> consumableService.outbound(1L, 200, "REF001", "超量出库", 1L));
    }

    @Test
    @DisplayName("入库操作增加库存")
    void inboundIncreasesStock() {
        when(consumableMapper.selectById(1L)).thenReturn(testEntity);
        when(consumableMapper.updateById(any())).thenReturn(1);
        when(transactionMapper.insert(any())).thenReturn(1);

        consumableService.inbound(1L, 50, "REF002", "入库测试", 1L);

        ArgumentCaptor<ConsumableEntity> captor = ArgumentCaptor.forClass(ConsumableEntity.class);
        verify(consumableMapper).updateById(captor.capture());
        assertEquals(150, captor.getValue().getStockQuantity()); // 100 + 50
    }

    @Test
    @DisplayName("删除耗材（物理删除）")
    void deleteSuccess() {
        when(consumableMapper.deleteById(1L)).thenReturn(1);

        consumableService.delete(1L, 1L);

        verify(consumableMapper).deleteById(1L);
    }
}