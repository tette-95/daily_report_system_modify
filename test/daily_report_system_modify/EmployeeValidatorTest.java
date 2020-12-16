package daily_report_system_modify;



import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import models.Employee;
import models.validators.EmployeeValidator;

public class EmployeeValidatorTest {

    @Before
    public void setUp() throws Exception {
        EmployeeValidator ev = new EmployeeValidator();

    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void test(){
        assertEquals("社員番号を入力してください。", EmployeeValidator._validateCode("", true));
        assertEquals("入力された社員番号の情報はすでに存在しています。", EmployeeValidator._validateCode("1234a", true));

        Employee e = new Employee();

        assertThat(EmployeeValidator.validate(e, true, true), CoreMatchers.not(is(empty())));
    }

    private Object empty() {
        // TODO 自動生成されたメソッド・スタブ
        return null;
    }
}
