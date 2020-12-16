package controllers.login;

import java.io.IOException;
import java.sql.Timestamp;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import models.Employee;
import utils.DBUtil;
import utils.EncryptUtil;

/**
 * Servlet implementation class LoginServlet
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public LoginServlet() {
        super();
        // TODO Auto-generated constructor stub
    }


    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    // ログイン画面を表示
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("_token", request.getSession().getId());
        request.setAttribute("hasError", false);
        request.setAttribute("login", false);

        if(request.getSession().getAttribute("flush") != null) {
            request.setAttribute("flush", request.getSession().getAttribute("flush"));
            request.getSession().removeAttribute("flush");
        }

        RequestDispatcher rd = request.getRequestDispatcher("/WEB-INF/views/login/login.jsp");
        rd.forward(request, response);
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    // ログイン処理を実行
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 認証結果を格納する変数
        String code = request.getParameter("code");
        String plain_pass = request.getParameter("password");

        Boolean check_result = false;
        Boolean check_code = false;
        Boolean tenMinutes = false;

        Employee eb = null;
        Employee e_code = null;

        if(code != null && !code.equals("") && plain_pass != null && !plain_pass.equals("")) {
            EntityManager em = DBUtil.createEntityManager();
            String password = EncryptUtil.getPasswordEncrypt(
                    plain_pass,
                    (String)this.getServletContext().getAttribute("pepper")
                    );
            //パスワードと社員番号を確認
            eb = check_result(code, password, eb, em);

            if(eb != null){
                check_result = true;
                eb = em.find(Employee.class, eb.getId());
            }else{
              //社員番号を確認
                e_code = check_code(code, e_code, em);
                if(e_code != null){
                    check_code = true;

                }
            }
        }

          //パスワードか社員番号、もしくは両方を間違えてる
           if(!check_result) {
               //社員番号だけ合ってる
               if(check_code){
                 //10分経過してるか確認
                   if(tenMinutes(e_code.getMistake_at(), e_code, tenMinutes)){
                       EntityManager em = DBUtil.createEntityManager();
                       e_code = em.find(Employee.class, e_code.getId());
                       e_code.setMistake_counts(e_code.getMistake_counts() + 1);
                       em.getTransaction().begin();
                       em.getTransaction().commit();

                     //カウントが５
                       if(e_code.getMistake_counts() == 5){
                           lock(e_code, em);
                           request.setAttribute("lock", true);
                       }
                       em.close();
                   }else{
                   request.setAttribute("lock", true);
                   }
               }
               request.setAttribute("_token", request.getSession().getId());
               request.setAttribute("hasError", true);
               request.setAttribute("code", code);

               RequestDispatcher rd = request.getRequestDispatcher("/WEB-INF/views/login/login.jsp");
               rd.forward(request, response);

           }else{
               if(tenMinutes(eb.getMistake_at(), eb, tenMinutes)){
                   // 認証できたらログイン状態にしてトップページへリダイレクト
                   EntityManager em = DBUtil.createEntityManager();
                   eb = em.find(Employee.class, eb.getId());
                   eb.setMistake_counts(0);
                   em.getTransaction().begin();
                   em.getTransaction().commit();
                   em.close();
                   request.getSession().setAttribute("login_employee", eb);
                   request.getSession().setAttribute("flush", "ログインしました。");
                   response.sendRedirect(request.getContextPath() + "/");
               }else{
                   request.setAttribute("lock", true);
                   request.setAttribute("_token", request.getSession().getId());
                   request.setAttribute("hasError", true);
                   request.setAttribute("code", code);

                   RequestDispatcher rd = request.getRequestDispatcher("/WEB-INF/views/login/login.jsp");
                   rd.forward(request, response);
               }
           }



    }




    //入力された社員番号とパスワードが登録されているか確認
    Employee check_result(String code, String password, Employee eb, EntityManager em){
        try {
            eb = em.createNamedQuery("checkLoginCodeAndPassword", Employee.class)
                  .setParameter("code", code)
                  .setParameter("pass", password)
                  .getSingleResult();
        } catch(NoResultException ex) {}
        return eb;
    }


    //入力された社員番号が登録されているか確認
    Employee check_code(String code, Employee e_code, EntityManager em){
        try {
            e_code = em.createNamedQuery("checkLoginCode", Employee.class)
                  .setParameter("code", code)
                  .getSingleResult();
        } catch(NoResultException ex) {}
        return e_code;
    }


    //10分経過したか確認
    boolean tenMinutes(Timestamp mistake_at, Employee e, boolean tenMinutes){
        System.out.println(mistake_at);
        System.out.println(mistake_at.getTime());
        long comparison = mistake_at.getTime() + 600000;
        System.out.println(comparison);
        Timestamp now = new Timestamp(System.currentTimeMillis());
        System.out.println(now);
        long n = now.getTime();
        System.out.println(n);
        if(n >= comparison){
             tenMinutes = true;
        }
        return tenMinutes;
    }


    //DBのmistake_countに１を足す
    void dbPlus1(Employee e_code, EntityManager em){
        System.out.println("てるお");
        e_code.setMistake_counts(e_code.getMistake_counts() + 1);
        em.getTransaction().begin();
        em.getTransaction().commit();
    }


    //5間違えた回のでロックする
    void lock(Employee e_code, EntityManager em){
        e_code.setMistake_counts(0);
        e_code.setMistake_at(new Timestamp(System.currentTimeMillis()));
        em.getTransaction().begin();
        em.getTransaction().commit();

    }
}
