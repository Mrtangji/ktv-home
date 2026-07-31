import{f as i,_ as y,a as m,C as p,o as u,c as d,k as a,D as o,g as t,h as r,z as n,t as k,p as h}from"./index-B87gEFAX.js";/**
 * @license lucide-vue-next v0.577.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const f=i("list-music",[["path",{d:"M16 5H3",key:"m91uny"}],["path",{d:"M11 12H3",key:"51ecnj"}],["path",{d:"M11 19H3",key:"zflm78"}],["path",{d:"M21 16V5",key:"yxg4q8"}],["circle",{cx:"18",cy:"16",r:"3",key:"1hluhg"}]]);/**
 * @license lucide-vue-next v0.577.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const _=i("radio",[["path",{d:"M16.247 7.761a6 6 0 0 1 0 8.478",key:"1fwjs5"}],["path",{d:"M19.075 4.933a10 10 0 0 1 0 14.134",key:"ehdyv1"}],["path",{d:"M4.925 19.067a10 10 0 0 1 0-14.134",key:"1q22gi"}],["path",{d:"M7.753 16.239a6 6 0 0 1 0-8.478",key:"r2q7qm"}],["circle",{cx:"12",cy:"12",r:"2",key:"1c9p78"}]]);/**
 * @license lucide-vue-next v0.577.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const b=i("search",[["path",{d:"m21 21-4.34-4.34",key:"14j7rj"}],["circle",{cx:"11",cy:"11",r:"8",key:"4ej97u"}]]),x={class:"tabbar"},M={key:0,class:"badge"},q={__name:"TabBar",props:{active:{type:String,default:""}},setup(s){const l=m();return(v,e)=>{const c=p("router-link");return u(),d("nav",x,[a(c,{class:n(["tab",{on:s.active==="home"}]),to:{name:"home"}},{default:o(()=>[a(t(b),{class:"ic",size:19}),e[0]||(e[0]=r("点歌 ",-1))]),_:1},8,["class"]),a(c,{class:n(["tab",{on:s.active==="queue"}]),to:{name:"queue"}},{default:o(()=>[a(t(f),{class:"ic",size:19}),e[1]||(e[1]=r("已点 ",-1)),t(l).queueCount?(u(),d("span",M,k(t(l).queueCount),1)):h("",!0)]),_:1},8,["class"]),a(c,{class:n(["tab",{on:s.active==="remote"}]),to:{name:"remote"}},{default:o(()=>[a(t(_),{class:"ic",size:19}),e[2]||(e[2]=r("遥控 ",-1))]),_:1},8,["class"])])}}},C=y(q,[["__scopeId","data-v-9a99e66b"]]);export{f as L,b as S,C as T};
