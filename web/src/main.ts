import Vue from 'vue';
import App from './App.vue';
import router from './router';

Vue.config.productionTip = false;

// console.log('~~ ' + process.env.VUE_APP_ENVTEST);

new Vue({
  router,
  render: (h) => h(App),
}).$mount('#app');
