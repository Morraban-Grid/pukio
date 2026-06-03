package com.pukio.analytics.etl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.IsoFields;

/**
 * Poblador de la dimensión tiempo (dim_time) del Data Warehouse.
 * 
 * Se ejecuta automáticamente al arrancar el módulo pukio-analytics si la tabla
 * dim_time está vacía. Genera registros para un rango de años configurado.
 * 
 * REQ 2.7 — Analytics Server SHALL populate dim_time with records for a range of years.
 */
@Component
public class DimTimePopulator implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DimTimePopulator.class);
    
    private static final int DEFAULT_FROM_YEAR = 2020;
    private static final int DEFAULT_TO_YEAR = 2030;
    
    private final JdbcTemplate jdbcTemplate;

    public DimTimePopulator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Verificando población de dim_time...");
        
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM dim_time", 
            Long.class
        );
        
        if (count != null && count > 0) {
            log.info("dim_time ya contiene {} registros. Omitiendo población.", count);
            return;
        }
        
        log.info("dim_time está vacía. Iniciando población para años {}-{}...", 
            DEFAULT_FROM_YEAR, DEFAULT_TO_YEAR);
        
        int inserted = populateDimTime(DEFAULT_FROM_YEAR, DEFAULT_TO_YEAR);
        
        log.info("Población de dim_time completada. {} registros insertados.", inserted);
    }

    /**
     * Puebla la tabla dim_time con registros para cada día del rango de años especificado.
     * 
     * @param fromYear año de inicio (inclusive)
     * @param toYear año de fin (inclusive)
     * @return cantidad de registros insertados
     */
    public int populateDimTime(int fromYear, int toYear) {
        if (fromYear > toYear) {
            throw new IllegalArgumentException(
                "fromYear debe ser menor o igual a toYear: " + fromYear + " > " + toYear
            );
        }
        
        LocalDate startDate = LocalDate.of(fromYear, 1, 1);
        LocalDate endDate = LocalDate.of(toYear, 12, 31);
        
        int insertedCount = 0;
        LocalDate currentDate = startDate;
        
        String insertSql = """
            INSERT INTO dim_time (
                full_date, 
                year, 
                quarter, 
                month, 
                week, 
                day_of_month, 
                day_of_week, 
                is_weekend, 
                is_holiday
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (full_date) DO NOTHING
        """;
        
        while (!currentDate.isAfter(endDate)) {
            int year = currentDate.getYear();
            int quarter = currentDate.get(IsoFields.QUARTER_OF_YEAR);
            int month = currentDate.getMonthValue();
            int week = currentDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            int dayOfMonth = currentDate.getDayOfMonth();
            int dayOfWeek = currentDate.getDayOfWeek().getValue(); // 1=Monday, 7=Sunday
            boolean isWeekend = (dayOfWeek == DayOfWeek.SATURDAY.getValue() 
                               || dayOfWeek == DayOfWeek.SUNDAY.getValue());
            boolean isHoliday = false; // Por defecto false; se puede configurar externamente
            
            try {
                jdbcTemplate.update(
                    insertSql,
                    java.sql.Date.valueOf(currentDate),
                    year,
                    quarter,
                    month,
                    week,
                    dayOfMonth,
                    dayOfWeek,
                    isWeekend,
                    isHoliday
                );
                insertedCount++;
                
                // Log cada año completado
                if (currentDate.getDayOfYear() == 1 && currentDate.getYear() > fromYear) {
                    log.debug("Poblado año {} completado.", currentDate.getYear() - 1);
                }
            } catch (Exception e) {
                log.error("Error insertando fecha {}: {}", currentDate, e.getMessage());
            }
            
            currentDate = currentDate.plusDays(1);
        }
        
        return insertedCount;
    }
    
    /**
     * Marca fechas específicas como feriados.
     * 
     * @param holidays array de fechas en formato ISO (YYYY-MM-DD)
     */
    public void markHolidays(String[] holidays) {
        String updateSql = "UPDATE dim_time SET is_holiday = TRUE WHERE full_date = ?";
        
        for (String holiday : holidays) {
            try {
                LocalDate holidayDate = LocalDate.parse(holiday);
                int updated = jdbcTemplate.update(updateSql, java.sql.Date.valueOf(holidayDate));
                if (updated > 0) {
                    log.debug("Marcado como feriado: {}", holiday);
                }
            } catch (Exception e) {
                log.error("Error marcando feriado {}: {}", holiday, e.getMessage());
            }
        }
    }
}
